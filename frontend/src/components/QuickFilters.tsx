import React from 'react';
import { Box, Chip } from '@mui/material';
import { FilterGroup } from './FilterBuilder';

export interface QuickFilter {
  id: string;
  label: string;
  filters: FilterGroup[];
  color?: 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';
}

interface QuickFiltersProps {
  quickFilters: QuickFilter[];
  activeFilterId?: string;
  onApply: (filters: FilterGroup[], filterId: string) => void;
  onClear: () => void;
}

const QuickFilters: React.FC<QuickFiltersProps> = ({
  quickFilters,
  activeFilterId,
  onApply,
  onClear,
}) => {
  const handleClick = (filter: QuickFilter) => {
    if (activeFilterId === filter.id) {
      onClear();
    } else {
      onApply(filter.filters, filter.id);
    }
  };

  return (
    <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 2 }}>
      {quickFilters.map((filter) => (
        <Chip
          key={filter.id}
          label={filter.label}
          onClick={() => handleClick(filter)}
          color={activeFilterId === filter.id ? filter.color || 'primary' : 'default'}
          variant={activeFilterId === filter.id ? 'filled' : 'outlined'}
          sx={{ cursor: 'pointer' }}
        />
      ))}
    </Box>
  );
};

export default QuickFilters;
