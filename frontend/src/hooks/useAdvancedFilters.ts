import { useState, useCallback, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { FilterGroup } from '../components/FilterBuilder';

export interface SortConfig {
  field: string;
  direction: 'asc' | 'desc';
}

export interface UseAdvancedFiltersReturn {
  filters: FilterGroup[];
  sort: SortConfig[];
  setFilters: (filters: FilterGroup[]) => void;
  setSort: (sort: SortConfig[]) => void;
  clearFilters: () => void;
  clearSort: () => void;
  applyFiltersToParams: (baseParams: URLSearchParams) => URLSearchParams;
  applySortToParams: (baseParams: URLSearchParams) => URLSearchParams;
}

/**
 * Custom hook for managing advanced filters, sorting, and URL state
 */
export const useAdvancedFilters = (
  moduleName: string
): UseAdvancedFiltersReturn => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [filters, setFiltersState] = useState<FilterGroup[]>([]);
  const [sort, setSortState] = useState<SortConfig[]>([]);

  // Load state from URL on mount
  useEffect(() => {
    const filtersParam = searchParams.get('filters');
    const sortParam = searchParams.get('sort');

    if (filtersParam) {
      try {
        const decodedFilters = JSON.parse(decodeURIComponent(filtersParam));
        setFiltersState(decodedFilters);
      } catch (e) {
        console.error('Failed to parse filters from URL', e);
      }
    }

    if (sortParam) {
      try {
        const decodedSort = JSON.parse(decodeURIComponent(sortParam));
        setSortState(decodedSort);
      } catch (e) {
        console.error('Failed to parse sort from URL', e);
      }
    }
  }, []);

  // Update URL when filters change
  const setFilters = useCallback(
    (newFilters: FilterGroup[]) => {
      setFiltersState(newFilters);
      const newParams = new URLSearchParams(searchParams);

      if (newFilters.length > 0) {
        newParams.set('filters', encodeURIComponent(JSON.stringify(newFilters)));
      } else {
        newParams.delete('filters');
      }

      setSearchParams(newParams);
    },
    [searchParams, setSearchParams]
  );

  // Update URL when sort changes
  const setSort = useCallback(
    (newSort: SortConfig[]) => {
      setSortState(newSort);
      const newParams = new URLSearchParams(searchParams);

      if (newSort.length > 0) {
        newParams.set('sort', encodeURIComponent(JSON.stringify(newSort)));
      } else {
        newParams.delete('sort');
      }

      setSearchParams(newParams);
    },
    [searchParams, setSearchParams]
  );

  const clearFilters = useCallback(() => {
    setFilters([]);
  }, [setFilters]);

  const clearSort = useCallback(() => {
    setSort([]);
  }, [setSort]);

  // Convert filters to API query parameters
  const applyFiltersToParams = useCallback(
    (baseParams: URLSearchParams): URLSearchParams => {
      const params = new URLSearchParams(baseParams);

      filters.forEach((group, groupIndex) => {
        group.conditions.forEach((condition, condIndex) => {
          if (condition.value !== '' && condition.value !== null && condition.value !== undefined) {
            const filterKey = `filter[${groupIndex}][${condIndex}][${condition.field}]`;
            params.set(filterKey, condition.operator);
            params.set(`${filterKey}_value`, String(condition.value));
          }
        });
        params.set(`filter[${groupIndex}][logic]`, group.logic);
      });

      return params;
    },
    [filters]
  );

  // Convert sort to API query parameters
  const applySortToParams = useCallback(
    (baseParams: URLSearchParams): URLSearchParams => {
      const params = new URLSearchParams(baseParams);

      if (sort.length > 0) {
        const sortString = sort.map((s) => `${s.field},${s.direction}`).join(';');
        params.set('sort', sortString);
      }

      return params;
    },
    [sort]
  );

  return {
    filters,
    sort,
    setFilters,
    setSort,
    clearFilters,
    clearSort,
    applyFiltersToParams,
    applySortToParams,
  };
};
