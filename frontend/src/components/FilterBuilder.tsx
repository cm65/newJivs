import React, { useState } from 'react';
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import {
  Add as AddIcon,
  Delete as DeleteIcon,
  FilterList as FilterIcon,
} from '@mui/icons-material';

export interface FilterCondition {
  id: string;
  field: string;
  operator: string;
  value: string | number | string[];
}

export interface FilterGroup {
  id: string;
  logic: 'AND' | 'OR';
  conditions: FilterCondition[];
}

interface FilterBuilderProps {
  fields: Array<{ value: string; label: string; type: string }>;
  filters: FilterGroup[];
  onApply: (filters: FilterGroup[]) => void;
  onClear: () => void;
}

const operators = {
  string: [
    { value: 'equals', label: 'Equals' },
    { value: 'not_equals', label: 'Not Equals' },
    { value: 'contains', label: 'Contains' },
    { value: 'not_contains', label: 'Does Not Contain' },
    { value: 'starts_with', label: 'Starts With' },
    { value: 'ends_with', label: 'Ends With' },
  ],
  number: [
    { value: 'equals', label: 'Equals' },
    { value: 'not_equals', label: 'Not Equals' },
    { value: 'greater_than', label: 'Greater Than' },
    { value: 'greater_than_or_equal', label: 'Greater Than or Equal' },
    { value: 'less_than', label: 'Less Than' },
    { value: 'less_than_or_equal', label: 'Less Than or Equal' },
    { value: 'between', label: 'Between' },
  ],
  date: [
    { value: 'equals', label: 'Equals' },
    { value: 'before', label: 'Before' },
    { value: 'after', label: 'After' },
    { value: 'between', label: 'Between' },
  ],
  enum: [
    { value: 'equals', label: 'Equals' },
    { value: 'not_equals', label: 'Not Equals' },
    { value: 'in_list', label: 'In List' },
    { value: 'not_in_list', label: 'Not In List' },
  ],
};

const FilterBuilder: React.FC<FilterBuilderProps> = ({
  fields,
  filters: initialFilters,
  onApply,
  onClear,
}) => {
  const [open, setOpen] = useState(false);
  const [filters, setFilters] = useState<FilterGroup[]>(initialFilters);

  const handleOpen = () => {
    setFilters(initialFilters.length > 0 ? initialFilters : [createNewGroup()]);
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
  };

  const createNewGroup = (): FilterGroup => ({
    id: Date.now().toString(),
    logic: 'AND',
    conditions: [createNewCondition()],
  });

  const createNewCondition = (): FilterCondition => ({
    id: Date.now().toString(),
    field: fields[0]?.value || '',
    operator: 'equals',
    value: '',
  });

  const addGroup = () => {
    setFilters([...filters, createNewGroup()]);
  };

  const removeGroup = (groupId: string) => {
    setFilters(filters.filter((g) => g.id !== groupId));
  };

  const addCondition = (groupId: string) => {
    setFilters(
      filters.map((group) =>
        group.id === groupId
          ? { ...group, conditions: [...group.conditions, createNewCondition()] }
          : group
      )
    );
  };

  const removeCondition = (groupId: string, conditionId: string) => {
    setFilters(
      filters.map((group) =>
        group.id === groupId
          ? {
              ...group,
              conditions: group.conditions.filter((c) => c.id !== conditionId),
            }
          : group
      )
    );
  };

  const updateGroupLogic = (groupId: string, logic: 'AND' | 'OR') => {
    setFilters(filters.map((group) => (group.id === groupId ? { ...group, logic } : group)));
  };

  const updateCondition = (
    groupId: string,
    conditionId: string,
    field: keyof FilterCondition,
    value: any
  ) => {
    setFilters(
      filters.map((group) =>
        group.id === groupId
          ? {
              ...group,
              conditions: group.conditions.map((condition) =>
                condition.id === conditionId ? { ...condition, [field]: value } : condition
              ),
            }
          : group
      )
    );
  };

  const handleApply = () => {
    onApply(filters);
    handleClose();
  };

  const handleClearAll = () => {
    setFilters([createNewGroup()]);
    onClear();
    handleClose();
  };

  const getOperatorsForField = (fieldValue: string) => {
    const field = fields.find((f) => f.value === fieldValue);
    return operators[field?.type as keyof typeof operators] || operators.string;
  };

  const activeFilterCount = initialFilters.reduce(
    (count, group) => count + group.conditions.filter((c) => c.value !== '').length,
    0
  );

  return (
    <>
      <Button
        variant={activeFilterCount > 0 ? 'contained' : 'outlined'}
        startIcon={<FilterIcon />}
        onClick={handleOpen}
        sx={{ mr: 2 }}
      >
        Filters {activeFilterCount > 0 && `(${activeFilterCount})`}
      </Button>

      <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
        <DialogTitle>Advanced Filters</DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 2 }}>
            {filters.map((group, groupIndex) => (
              <Paper
                key={group.id}
                variant="outlined"
                sx={{ p: 2, mb: 2, backgroundColor: 'grey.50' }}
              >
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                  <ToggleButtonGroup
                    value={group.logic}
                    exclusive
                    onChange={(_, value) => value && updateGroupLogic(group.id, value)}
                    size="small"
                  >
                    <ToggleButton value="AND">AND</ToggleButton>
                    <ToggleButton value="OR">OR</ToggleButton>
                  </ToggleButtonGroup>
                  {filters.length > 1 && (
                    <IconButton size="small" onClick={() => removeGroup(group.id)}>
                      <DeleteIcon />
                    </IconButton>
                  )}
                </Box>

                {group.conditions.map((condition, conditionIndex) => (
                  <Box key={condition.id} sx={{ display: 'flex', gap: 1, mb: 1 }}>
                    <FormControl sx={{ minWidth: 150 }}>
                      <InputLabel size="small">Field</InputLabel>
                      <Select
                        size="small"
                        value={condition.field}
                        label="Field"
                        onChange={(e) =>
                          updateCondition(group.id, condition.id, 'field', e.target.value)
                        }
                      >
                        {fields.map((field) => (
                          <MenuItem key={field.value} value={field.value}>
                            {field.label}
                          </MenuItem>
                        ))}
                      </Select>
                    </FormControl>

                    <FormControl sx={{ minWidth: 180 }}>
                      <InputLabel size="small">Operator</InputLabel>
                      <Select
                        size="small"
                        value={condition.operator}
                        label="Operator"
                        onChange={(e) =>
                          updateCondition(group.id, condition.id, 'operator', e.target.value)
                        }
                      >
                        {getOperatorsForField(condition.field).map((op) => (
                          <MenuItem key={op.value} value={op.value}>
                            {op.label}
                          </MenuItem>
                        ))}
                      </Select>
                    </FormControl>

                    <TextField
                      size="small"
                      label="Value"
                      value={condition.value}
                      onChange={(e) =>
                        updateCondition(group.id, condition.id, 'value', e.target.value)
                      }
                      sx={{ flexGrow: 1 }}
                    />

                    {group.conditions.length > 1 && (
                      <IconButton
                        size="small"
                        onClick={() => removeCondition(group.id, condition.id)}
                      >
                        <DeleteIcon />
                      </IconButton>
                    )}
                  </Box>
                ))}

                <Button
                  size="small"
                  startIcon={<AddIcon />}
                  onClick={() => addCondition(group.id)}
                  sx={{ mt: 1 }}
                >
                  Add Condition
                </Button>
              </Paper>
            ))}

            <Button variant="outlined" startIcon={<AddIcon />} onClick={addGroup} fullWidth>
              Add Filter Group
            </Button>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleClearAll} color="error">
            Clear All
          </Button>
          <Button onClick={handleClose}>Cancel</Button>
          <Button onClick={handleApply} variant="contained">
            Apply Filters
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default FilterBuilder;
