import React, { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TableSortLabel,
  TextField,
  Tooltip,
  Typography,
  Alert,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import {
  Add as AddIcon,
  PlayArrow as PlayIcon,
  Stop as StopIcon,
  Delete as DeleteIcon,
  Refresh as RefreshIcon,
  Visibility as ViewIcon,
  Assessment as StatsIcon,
  ArrowUpward as ArrowUpIcon,
  ArrowDownward as ArrowDownIcon,
} from '@mui/icons-material';
import extractionService, { Extraction, ExtractionConfig } from '../services/extractionService';
import FilterBuilder, { FilterGroup } from '../components/FilterBuilder';
import QuickFilters, { QuickFilter } from '../components/QuickFilters';
import SavedViews from '../components/SavedViews';
import { useAdvancedFilters, SortConfig } from '../hooks/useAdvancedFilters';

const Extractions: React.FC = () => {
  const [extractions, setExtractions] = useState<Extraction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);
  const [totalElements, setTotalElements] = useState(0);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [newExtraction, setNewExtraction] = useState<Partial<ExtractionConfig>>({
    name: '',
    sourceType: 'JDBC',
    connectionConfig: {},
  });
  const [activeQuickFilter, setActiveQuickFilter] = useState<string | undefined>();

  // Advanced filters and sorting
  const { filters, sort, setFilters, setSort, clearFilters, clearSort } =
    useAdvancedFilters('extractions');

  // Define filterable fields
  const filterFields = [
    { value: 'name', label: 'Name', type: 'string' },
    { value: 'sourceType', label: 'Source Type', type: 'enum' },
    { value: 'status', label: 'Status', type: 'enum' },
    { value: 'recordsExtracted', label: 'Records Extracted', type: 'number' },
    { value: 'createdAt', label: 'Created Date', type: 'date' },
  ];

  // Define quick filters
  const quickFilters: QuickFilter[] = [
    {
      id: 'active',
      label: 'Active',
      color: 'info',
      filters: [
        {
          id: '1',
          logic: 'OR',
          conditions: [
            { id: '1', field: 'status', operator: 'equals', value: 'RUNNING' },
            { id: '2', field: 'status', operator: 'equals', value: 'PENDING' },
          ],
        },
      ],
    },
    {
      id: 'failed',
      label: 'Failed',
      color: 'error',
      filters: [
        {
          id: '1',
          logic: 'AND',
          conditions: [{ id: '1', field: 'status', operator: 'equals', value: 'FAILED' }],
        },
      ],
    },
    {
      id: 'completed_today',
      label: 'Completed Today',
      color: 'success',
      filters: [
        {
          id: '1',
          logic: 'AND',
          conditions: [
            { id: '1', field: 'status', operator: 'equals', value: 'COMPLETED' },
            {
              id: '2',
              field: 'createdAt',
              operator: 'after',
              value: new Date().toISOString().split('T')[0],
            },
          ],
        },
      ],
    },
    {
      id: 'high_volume',
      label: 'High Volume (>10k records)',
      color: 'warning',
      filters: [
        {
          id: '1',
          logic: 'AND',
          conditions: [
            { id: '1', field: 'recordsExtracted', operator: 'greater_than', value: 10000 },
          ],
        },
      ],
    },
  ];

  useEffect(() => {
    loadExtractions();
  }, [page, rowsPerPage, filters, sort]);

  const loadExtractions = async () => {
    try {
      setLoading(true);
      // For now, we'll ignore advanced filters in the API call
      // In production, you'd convert filters and sort to API params
      const response = await extractionService.getExtractions(page, rowsPerPage);
      setExtractions(response.content || []);
      setTotalElements(response.totalElements || 0);
      setError(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load extractions');
    } finally {
      setLoading(false);
    }
  };

  const handleStartExtraction = async (id: string) => {
    try {
      await extractionService.startExtraction(id);
      loadExtractions();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to start extraction');
    }
  };

  const handleStopExtraction = async (id: string) => {
    try {
      await extractionService.stopExtraction(id);
      loadExtractions();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to stop extraction');
    }
  };

  const handleDeleteExtraction = async (id: string) => {
    if (window.confirm('Are you sure you want to delete this extraction?')) {
      try {
        await extractionService.deleteExtraction(id);
        loadExtractions();
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to delete extraction');
      }
    }
  };

  const handleCreateExtraction = async () => {
    try {
      await extractionService.createExtraction(newExtraction as ExtractionConfig);
      setCreateDialogOpen(false);
      setNewExtraction({ name: '', sourceType: 'JDBC', connectionConfig: {} });
      loadExtractions();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create extraction');
    }
  };

  const handleSort = (field: string, shiftKey: boolean) => {
    const existingSort = sort.find((s) => s.field === field);
    let newSort: SortConfig[];

    if (shiftKey) {
      // Multi-column sort
      if (existingSort) {
        // Toggle direction
        newSort = sort.map((s) =>
          s.field === field ? { ...s, direction: s.direction === 'asc' ? 'desc' : 'asc' } : s
        );
      } else {
        // Add new sort column
        newSort = [...sort, { field, direction: 'asc' }];
      }
    } else {
      // Single column sort
      if (existingSort && existingSort.direction === 'asc') {
        newSort = [{ field, direction: 'desc' }];
      } else if (existingSort && existingSort.direction === 'desc') {
        newSort = [];
      } else {
        newSort = [{ field, direction: 'asc' }];
      }
    }

    setSort(newSort);
  };

  const getSortDirection = (field: string): 'asc' | 'desc' | false => {
    const sortConfig = sort.find((s) => s.field === field);
    return sortConfig ? sortConfig.direction : false;
  };

  const getSortIndex = (field: string): number => {
    const index = sort.findIndex((s) => s.field === field);
    return index >= 0 ? index + 1 : 0;
  };

  const handleApplyFilters = (newFilters: FilterGroup[]) => {
    setFilters(newFilters);
    setActiveQuickFilter(undefined);
    setPage(0);
  };

  const handleClearFilters = () => {
    clearFilters();
    setActiveQuickFilter(undefined);
    setPage(0);
  };

  const handleApplyQuickFilter = (newFilters: FilterGroup[], filterId: string) => {
    setFilters(newFilters);
    setActiveQuickFilter(filterId);
    setPage(0);
  };

  const handleApplySavedView = (view: any) => {
    setFilters(view.filters);
    setSort(view.sort);
    setActiveQuickFilter(undefined);
    setPage(0);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return 'success';
      case 'RUNNING':
        return 'info';
      case 'FAILED':
        return 'error';
      case 'PENDING':
        return 'warning';
      default:
        return 'default';
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold" gutterBottom>
            Data Extractions
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Manage and monitor data extraction jobs
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={loadExtractions}>
            Refresh
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setCreateDialogOpen(true)}
          >
            New Extraction
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" gutterBottom>
                Total Extractions
              </Typography>
              <Typography variant="h4">{totalElements}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" gutterBottom>
                Running
              </Typography>
              <Typography variant="h4">
                {extractions.filter((e) => e.status === 'RUNNING').length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" gutterBottom>
                Completed
              </Typography>
              <Typography variant="h4">
                {extractions.filter((e) => e.status === 'COMPLETED').length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" gutterBottom>
                Failed
              </Typography>
              <Typography variant="h4">
                {extractions.filter((e) => e.status === 'FAILED').length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Quick Filters */}
      <QuickFilters
        quickFilters={quickFilters}
        activeFilterId={activeQuickFilter}
        onApply={handleApplyQuickFilter}
        onClear={handleClearFilters}
      />

      {/* Advanced Filters and Saved Views */}
      <Box sx={{ display: 'flex', gap: 2, mb: 2, alignItems: 'center' }}>
        <FilterBuilder
          fields={filterFields}
          filters={filters}
          onApply={handleApplyFilters}
          onClear={handleClearFilters}
        />
        <SavedViews
          module="extractions"
          currentFilters={filters}
          currentSort={sort}
          onApply={handleApplySavedView}
        />
        {sort.length > 0 && (
          <Chip
            label={`Sorted by ${sort.length} column${sort.length > 1 ? 's' : ''}`}
            onDelete={clearSort}
            color="primary"
            variant="outlined"
          />
        )}
      </Box>

      {/* Table */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>
                <TableSortLabel
                  active={getSortDirection('name') !== false}
                  direction={getSortDirection('name') || 'asc'}
                  onClick={(e) => handleSort('name', e.shiftKey)}
                  IconComponent={sort.length > 1 ? ArrowUpIcon : undefined}
                >
                  Name
                  {sort.length > 1 && getSortIndex('name') > 0 && (
                    <Box component="span" sx={{ ml: 0.5, fontSize: '0.75rem' }}>
                      {getSortIndex('name')}
                    </Box>
                  )}
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={getSortDirection('sourceType') !== false}
                  direction={getSortDirection('sourceType') || 'asc'}
                  onClick={(e) => handleSort('sourceType', e.shiftKey)}
                >
                  Source Type
                  {sort.length > 1 && getSortIndex('sourceType') > 0 && (
                    <Box component="span" sx={{ ml: 0.5, fontSize: '0.75rem' }}>
                      {getSortIndex('sourceType')}
                    </Box>
                  )}
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={getSortDirection('status') !== false}
                  direction={getSortDirection('status') || 'asc'}
                  onClick={(e) => handleSort('status', e.shiftKey)}
                >
                  Status
                  {sort.length > 1 && getSortIndex('status') > 0 && (
                    <Box component="span" sx={{ ml: 0.5, fontSize: '0.75rem' }}>
                      {getSortIndex('status')}
                    </Box>
                  )}
                </TableSortLabel>
              </TableCell>
              <TableCell align="right">
                <TableSortLabel
                  active={getSortDirection('recordsExtracted') !== false}
                  direction={getSortDirection('recordsExtracted') || 'asc'}
                  onClick={(e) => handleSort('recordsExtracted', e.shiftKey)}
                >
                  Records Extracted
                  {sort.length > 1 && getSortIndex('recordsExtracted') > 0 && (
                    <Box component="span" sx={{ ml: 0.5, fontSize: '0.75rem' }}>
                      {getSortIndex('recordsExtracted')}
                    </Box>
                  )}
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={getSortDirection('createdAt') !== false}
                  direction={getSortDirection('createdAt') || 'asc'}
                  onClick={(e) => handleSort('createdAt', e.shiftKey)}
                >
                  Created At
                  {sort.length > 1 && getSortIndex('createdAt') > 0 && (
                    <Box component="span" sx={{ ml: 0.5, fontSize: '0.75rem' }}>
                      {getSortIndex('createdAt')}
                    </Box>
                  )}
                </TableSortLabel>
              </TableCell>
              <TableCell align="center">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  <CircularProgress />
                </TableCell>
              </TableRow>
            ) : extractions.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  <Typography variant="body2" color="text.secondary">
                    No extractions found
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              extractions.map((extraction) => (
                <TableRow key={extraction.id} hover>
                  <TableCell>{extraction.name}</TableCell>
                  <TableCell>{extraction.sourceType || 'N/A'}</TableCell>
                  <TableCell>
                    <Chip
                      label={extraction.status}
                      color={getStatusColor(extraction.status) as any}
                      size="small"
                    />
                  </TableCell>
                  <TableCell align="right">
                    {(extraction.recordsExtracted || 0).toLocaleString()}
                  </TableCell>
                  <TableCell>
                    {extraction.createdAt ? new Date(extraction.createdAt).toLocaleString() : 'N/A'}
                  </TableCell>
                  <TableCell align="center">
                    <Tooltip title="View Details">
                      <IconButton size="small">
                        <ViewIcon />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Statistics">
                      <IconButton size="small">
                        <StatsIcon />
                      </IconButton>
                    </Tooltip>
                    {extraction.status === 'PENDING' && (
                      <Tooltip title="Start">
                        <IconButton
                          size="small"
                          color="primary"
                          onClick={() => handleStartExtraction(extraction.id)}
                        >
                          <PlayIcon />
                        </IconButton>
                      </Tooltip>
                    )}
                    {extraction.status === 'RUNNING' && (
                      <Tooltip title="Stop">
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => handleStopExtraction(extraction.id)}
                        >
                          <StopIcon />
                        </IconButton>
                      </Tooltip>
                    )}
                    <Tooltip title="Delete">
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => handleDeleteExtraction(extraction.id)}
                      >
                        <DeleteIcon />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
        <TablePagination
          component="div"
          count={totalElements}
          page={page}
          onPageChange={(_, newPage) => setPage(newPage)}
          rowsPerPage={rowsPerPage}
          rowsPerPageOptions={[10, 20, 50, 100]}
          onRowsPerPageChange={(e) => {
            setRowsPerPage(parseInt(e.target.value, 10));
            setPage(0);
          }}
        />
      </TableContainer>

      {/* Create Dialog */}
      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create New Extraction</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField
              label="Name"
              fullWidth
              value={newExtraction.name}
              onChange={(e) => setNewExtraction({ ...newExtraction, name: e.target.value })}
            />
            <FormControl fullWidth>
              <InputLabel>Source Type</InputLabel>
              <Select
                value={newExtraction.sourceType}
                label="Source Type"
                onChange={(e) =>
                  setNewExtraction({ ...newExtraction, sourceType: e.target.value as any })
                }
              >
                <MenuItem value="JDBC">JDBC Database</MenuItem>
                <MenuItem value="SAP">SAP System</MenuItem>
                <MenuItem value="FILE">File System</MenuItem>
                <MenuItem value="API">REST API</MenuItem>
              </Select>
            </FormControl>
            <TextField
              label="Extraction Query"
              fullWidth
              multiline
              rows={4}
              value={newExtraction.extractionQuery || ''}
              onChange={(e) =>
                setNewExtraction({ ...newExtraction, extractionQuery: e.target.value })
              }
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateDialogOpen(false)}>Cancel</Button>
          <Button
            onClick={handleCreateExtraction}
            variant="contained"
            disabled={!newExtraction.name}
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Extractions;
