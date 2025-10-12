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
} from '@mui/icons-material';
import extractionService, { Extraction, ExtractionConfig } from '../services/extractionService';

const Extractions: React.FC = () => {
  const [extractions, setExtractions] = useState<Extraction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);
  const [totalElements, setTotalElements] = useState(0);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [newExtraction, setNewExtraction] = useState<Partial<ExtractionConfig>>({
    name: '',
    sourceType: 'JDBC',
    connectionConfig: {},
  });

  useEffect(() => {
    loadExtractions();
  }, [page, rowsPerPage, statusFilter]);

  const loadExtractions = async () => {
    try {
      setLoading(true);
      const response = await extractionService.getExtractions(page, rowsPerPage, statusFilter);
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
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={loadExtractions}
          >
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
            <MenuItem value="COMPLETED">Completed</MenuItem>
            <MenuItem value="FAILED">Failed</MenuItem>
          </Select>
        </FormControl>
      </Box>

      {/* Table */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Source Type</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Records Extracted</TableCell>
              <TableCell>Created At</TableCell>
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
