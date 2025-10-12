import React, { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  IconButton,
  LinearProgress,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
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
  Pause as PauseIcon,
  Replay as ReplayIcon,
  Undo as UndoIcon,
  Delete as DeleteIcon,
  Refresh as RefreshIcon,
  Visibility as ViewIcon,
  Assessment as StatsIcon,
} from '@mui/icons-material';
import migrationService, { Migration, MigrationConfig } from '../services/migrationService';
import BulkOperationsToolbar from '../components/BulkOperationsToolbar';
import websocketService from '../services/websocket.service';

const Migrations: React.FC = () => {
  const [migrations, setMigrations] = useState<Migration[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);
  const [totalElements, setTotalElements] = useState(0);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [newMigration, setNewMigration] = useState<Partial<MigrationConfig>>({
    name: '',
    sourceConfig: {},
    targetConfig: {},
  });
  const [selectedIds, setSelectedIds] = useState<string[]>([]);

  useEffect(() => {
    loadMigrations();
  }, [page, rowsPerPage, statusFilter]);

  // WebSocket connection and subscription for real-time updates
  useEffect(() => {
    let subscriptionKey: string | null = null;
    let mounted = true;

    const connectAndSubscribe = async () => {
      try {
        if (!mounted) return; // Guard against unmount during async

        // Connect to WebSocket if not already connected
        if (!websocketService.isConnected()) {
          await websocketService.connect();
        }

        if (!mounted) return; // Check again after async operation

        // Subscribe to all migration updates
        subscriptionKey = websocketService.subscribeToAllMigrations((update) => {
          if (!mounted) return; // Ignore updates after unmount

          console.log('Received migration update:', update);

          // Update the migration in the list
          setMigrations((prevMigrations) =>
            prevMigrations.map((migration) =>
              migration.id === update.id
                ? {
                    ...migration,
                    status: update.status || migration.status,
                    phase: update.phase || migration.phase,
                    progress: update.progress !== undefined ? update.progress : migration.progress,
                    recordsMigrated: update.recordsMigrated || migration.recordsMigrated,
                    totalRecords: update.totalRecords || migration.totalRecords,
                  }
                : migration
            )
          );
        });
      } catch (error) {
        console.error('Failed to connect to WebSocket:', error);
      }
    };

    connectAndSubscribe();

    // Cleanup on unmount
    return () => {
      mounted = false; // Prevent state updates after unmount
      if (subscriptionKey) {
        websocketService.unsubscribe(subscriptionKey);
      }
    };
  }, []);

  const loadMigrations = async () => {
    try {
      setLoading(true);
      const response = await migrationService.getMigrations(page, rowsPerPage, statusFilter);
      setMigrations(response.content || []);
      setTotalElements(response.totalElements || 0);
      setError(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load migrations');
    } finally {
      setLoading(false);
    }
  };

  const handleStartMigration = async (id: string) => {
    try {
      await migrationService.startMigration(id);
      loadMigrations();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to start migration');
    }
  };

  const handlePauseMigration = async (id: string) => {
    try {
      await migrationService.pauseMigration(id);
      loadMigrations();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to pause migration');
    }
  };

  const handleResumeMigration = async (id: string) => {
    try {
      await migrationService.resumeMigration(id);
      loadMigrations();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to resume migration');
    }
  };

  const handleRollbackMigration = async (id: string) => {
    if (window.confirm('Are you sure you want to rollback this migration?')) {
      try {
        await migrationService.rollbackMigration(id);
        loadMigrations();
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to rollback migration');
      }
    }
  };

  const handleDeleteMigration = async (id: string) => {
    if (window.confirm('Are you sure you want to delete this migration?')) {
      try {
        await migrationService.deleteMigration(id);
        loadMigrations();
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to delete migration');
      }
    }
  };

  const handleCreateMigration = async () => {
    try {
      await migrationService.createMigration(newMigration as MigrationConfig);
      setCreateDialogOpen(false);
      setNewMigration({ name: '', sourceConfig: {}, targetConfig: {} });
      loadMigrations();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create migration');
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return 'success';
      case 'RUNNING':
        return 'info';
      case 'PAUSED':
        return 'warning';
      case 'FAILED':
        return 'error';
      case 'ROLLING_BACK':
        return 'warning';
      case 'PENDING':
        return 'default';
      default:
        return 'default';
    }
  };

  // Bulk operations handlers
  const handleSelectAll = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.checked) {
      setSelectedIds(migrations.map((m) => m.id));
    } else {
      setSelectedIds([]);
    }
  };

  const handleSelectOne = (id: string) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((selectedId) => selectedId !== id) : [...prev, id]
    );
  };

  const handleBulkStart = async (ids: string[]) => {
    try {
      await migrationService.bulkStart(ids);
      setSelectedIds([]);
      loadMigrations();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to start selected migrations');
    }
  };

  const handleBulkPause = async (ids: string[]) => {
    try {
      await migrationService.bulkPause(ids);
      setSelectedIds([]);
      loadMigrations();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to pause selected migrations');
    }
  };

  const handleBulkResume = async (ids: string[]) => {
    try {
      await migrationService.bulkResume(ids);
      setSelectedIds([]);
      loadMigrations();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to resume selected migrations');
    }
  };

  const handleBulkDelete = async (ids: string[]) => {
    try {
      await migrationService.bulkDelete(ids);
      setSelectedIds([]);
      loadMigrations();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete selected migrations');
    }
  };

  const handleBulkExport = async (ids: string[]) => {
    try {
      await migrationService.bulkExport(ids);
      setSelectedIds([]);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to export selected migrations');
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold" gutterBottom>
            Data Migrations
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Manage and monitor data migration jobs
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={loadMigrations}>
            Refresh
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setCreateDialogOpen(true)}
          >
            New Migration
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
                Total Migrations
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
                {migrations.filter((m) => m.status === 'RUNNING').length}
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
                {migrations.filter((m) => m.status === 'COMPLETED').length}
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
                {migrations.filter((m) => m.status === 'FAILED').length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Filter */}
      <Box sx={{ mb: 2 }}>
        <FormControl sx={{ minWidth: 200 }}>
          <InputLabel>Filter by Status</InputLabel>
          <Select
            value={statusFilter}
            label="Filter by Status"
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <MenuItem value="">All</MenuItem>
            <MenuItem value="PENDING">Pending</MenuItem>
            <MenuItem value="RUNNING">Running</MenuItem>
            <MenuItem value="PAUSED">Paused</MenuItem>
            <MenuItem value="COMPLETED">Completed</MenuItem>
            <MenuItem value="FAILED">Failed</MenuItem>
          </Select>
        </FormControl>
      </Box>

      {/* Bulk Operations Toolbar */}
      {selectedIds.length > 0 && (
        <BulkOperationsToolbar
          selectedIds={selectedIds}
          actions={[
            {
              id: 'start',
              label: 'Start',
              icon: <PlayIcon />,
              color: 'primary',
              onExecute: handleBulkStart,
            },
            {
              id: 'pause',
              label: 'Pause',
              icon: <PauseIcon />,
              color: 'warning',
              confirmMessage: 'Are you sure you want to pause the selected migrations?',
              onExecute: handleBulkPause,
            },
            {
              id: 'resume',
              label: 'Resume',
              icon: <ReplayIcon />,
              color: 'primary',
              onExecute: handleBulkResume,
            },
            {
              id: 'delete',
              label: 'Delete',
              icon: <DeleteIcon />,
              color: 'error',
              confirmMessage: 'Are you sure you want to delete the selected migrations? This action cannot be undone.',
              onExecute: handleBulkDelete,
            },
          ]}
          onClearSelection={() => setSelectedIds([])}
        />
      )}

      {/* Table */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell padding="checkbox">
                <Checkbox
                  indeterminate={selectedIds.length > 0 && selectedIds.length < migrations.length}
                  checked={migrations.length > 0 && selectedIds.length === migrations.length}
                  onChange={handleSelectAll}
                />
              </TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Phase</TableCell>
              <TableCell>Progress</TableCell>
              <TableCell align="right">Records Migrated</TableCell>
              <TableCell>Created At</TableCell>
              <TableCell align="center">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={8} align="center">
                  <CircularProgress />
                </TableCell>
              </TableRow>
            ) : migrations.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} align="center">
                  <Typography variant="body2" color="text.secondary">
                    No migrations found
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              migrations.map((migration) => (
                <TableRow key={migration.id} hover>
                  <TableCell padding="checkbox">
                    <Checkbox
                      checked={selectedIds.includes(migration.id)}
                      onChange={() => handleSelectOne(migration.id)}
                    />
                  </TableCell>
                  <TableCell>{migration.name}</TableCell>
                  <TableCell>
                    <Chip
                      label={migration.status}
                      color={getStatusColor(migration.status) as any}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>{migration.phase || 'N/A'}</TableCell>
                  <TableCell sx={{ width: '200px' }}>
                    <Box>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                        <Typography variant="caption">
                          {Math.min(100, Math.max(0, migration.progress || 0)).toFixed(1)}%
                        </Typography>
                        <Typography variant="caption">
                          {(migration.recordsMigrated || 0).toLocaleString()} / {(migration.totalRecords || 0).toLocaleString()}
                        </Typography>
                      </Box>
                      <LinearProgress
                        variant="determinate"
                        value={Math.min(100, Math.max(0, migration.progress || 0))}
                      />
                    </Box>
                  </TableCell>
                  <TableCell align="right">
                    {(migration.recordsMigrated || 0).toLocaleString()}
                  </TableCell>
                  <TableCell>
                    {migration.createdAt ? new Date(migration.createdAt).toLocaleString() : 'N/A'}
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
                    {migration.status === 'PENDING' && (
                      <Tooltip title="Start">
                        <IconButton
                          size="small"
                          color="primary"
                          onClick={() => handleStartMigration(migration.id)}
                        >
                          <PlayIcon />
                        </IconButton>
                      </Tooltip>
                    )}
                    {migration.status === 'RUNNING' && (
                      <Tooltip title="Pause">
                        <IconButton
                          size="small"
                          color="warning"
                          onClick={() => handlePauseMigration(migration.id)}
                        >
                          <PauseIcon />
                        </IconButton>
                      </Tooltip>
                    )}
                    {migration.status === 'PAUSED' && (
                      <Tooltip title="Resume">
                        <IconButton
                          size="small"
                          color="primary"
                          onClick={() => handleResumeMigration(migration.id)}
                        >
                          <ReplayIcon />
                        </IconButton>
                      </Tooltip>
                    )}
                    {(migration.status === 'COMPLETED' || migration.status === 'FAILED') && (
                      <Tooltip title="Rollback">
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => handleRollbackMigration(migration.id)}
                        >
                          <UndoIcon />
                        </IconButton>
                      </Tooltip>
                    )}
                    <Tooltip title="Delete">
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => handleDeleteMigration(migration.id)}
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
      <Dialog
        open={createDialogOpen}
        onClose={() => setCreateDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Create New Migration</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField
              label="Name"
              fullWidth
              value={newMigration.name}
              onChange={(e) => setNewMigration({ ...newMigration, name: e.target.value })}
            />
            <Typography variant="body2" color="text.secondary">
              Source Configuration
            </Typography>
            <TextField
              label="Source Connection"
              fullWidth
              placeholder="Enter source connection details"
            />
            <Typography variant="body2" color="text.secondary">
              Target Configuration
            </Typography>
            <TextField
              label="Target Connection"
              fullWidth
              placeholder="Enter target connection details"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateDialogOpen(false)}>Cancel</Button>
          <Button
            onClick={handleCreateMigration}
            variant="contained"
            disabled={!newMigration.name}
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Migrations;
