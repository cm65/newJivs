import { useState, useCallback } from 'react';

/**
 * Custom hook for managing bulk selection in tables
 * @template T - The type of items being selected (must have an 'id' property)
 */
export function useBulkSelection<T extends { id: string }>() {
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

  /**
   * Toggle selection for a single item
   */
  const toggleSelection = useCallback((id: string) => {
    setSelectedIds((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(id)) {
        newSet.delete(id);
      } else {
        newSet.add(id);
      }
      return newSet;
    });
  }, []);

  /**
   * Select all items from the provided list
   */
  const selectAll = useCallback((items: T[]) => {
    setSelectedIds(new Set(items.map((item) => item.id)));
  }, []);

  /**
   * Clear all selections
   */
  const clearSelection = useCallback(() => {
    setSelectedIds(new Set());
  }, []);

  /**
   * Check if a specific item is selected
   */
  const isSelected = useCallback(
    (id: string) => {
      return selectedIds.has(id);
    },
    [selectedIds]
  );

  /**
   * Check if all items are selected
   */
  const isAllSelected = useCallback(
    (items: T[]) => {
      if (items.length === 0) return false;
      return items.every((item) => selectedIds.has(item.id));
    },
    [selectedIds]
  );

  /**
   * Check if some (but not all) items are selected
   */
  const isSomeSelected = useCallback(
    (items: T[]) => {
      if (items.length === 0) return false;
      const selectedCount = items.filter((item) => selectedIds.has(item.id)).length;
      return selectedCount > 0 && selectedCount < items.length;
    },
    [selectedIds]
  );

  /**
   * Get count of selected items
   */
  const selectedCount = selectedIds.size;

  /**
   * Get array of selected IDs
   */
  const selectedIdsArray = Array.from(selectedIds);

  return {
    selectedIds,
    selectedIdsArray,
    selectedCount,
    toggleSelection,
    selectAll,
    clearSelection,
    isSelected,
    isAllSelected,
    isSomeSelected,
  };
}

export default useBulkSelection;
