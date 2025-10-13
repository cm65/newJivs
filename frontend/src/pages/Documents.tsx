import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  Button,
  IconButton,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  Alert,
  Grid,
  Card,
  CardContent,
  Tabs,
  Tab,
  LinearProgress,
  Tooltip,
  InputAdornment,
  FormControlLabel,
  Checkbox,
  Divider
} from '@mui/material';
import {
  CloudUpload,
  Search,
  Archive,
  Restore,
  Download,
  Delete,
  Visibility,
  FilterList,
  Refresh,
  FolderOpen,
  Storage
} from '@mui/icons-material';
import apiClient from '../services/apiClient';

interface Document {
  id: string;
  filename: string;
  title: string;
  description?: string;
  fileType: string;
  size: number;
  status: string;
  archived: boolean;
  storageTier?: string;
  tags?: string[];
  author?: string;
  createdDate: string;
  modifiedDate: string;
  checksum?: string;
  compressed?: boolean;
  encrypted?: boolean;
  compressionRatio?: number;
}

interface UploadDialogState {
  open: boolean;
  file: File | null;
  title: string;
  description: string;
  tags: string;
  archive: boolean;
  compress: boolean;
  encrypt: boolean;
}

interface SearchState {
  query: string;
  fileTypes: string[];
  archived: boolean | null;
  storageTier: string;
  author: string;
  tags: string;
}

interface ArchiveDialogState {
  open: boolean;
  documentIds: string[];
  archiveType: string;
  compress: boolean;
  encrypt: boolean;
  reason: string;
}

const Documents: React.FC = () => {
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalCount, setTotalCount] = useState(0);
  const [tabValue, setTabValue] = useState(0); // 0: Active, 1: Archived, 2: Search
  const [selectedDocuments, setSelectedDocuments] = useState<string[]>([]);

  // Statistics
  const [stats, setStats] = useState({
    totalDocuments: 0,
    activeDocuments: 0,
    archivedDocuments: 0,
    totalSize: 0,
    compressionSavings: 0
  });

  // Upload dialog state
  const [uploadDialog, setUploadDialog] = useState<UploadDialogState>({
    open: false,
    file: null,
    title: '',
    description: '',
    tags: '',
    archive: false,
    compress: false,
    encrypt: false
  });

  // Search state
  const [searchState, setSearchState] = useState<SearchState>({
    query: '',
    fileTypes: [],
    archived: null,
    storageTier: '',
    author: '',
    tags: ''
  });

  // Archive dialog state
  const [archiveDialog, setArchiveDialog] = useState<ArchiveDialogState>({
    open: false,
    documentIds: [],
    archiveType: 'HOT',
    compress: true,
    encrypt: false,
    reason: ''
  });

  // Load documents
  const loadDocuments = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: rowsPerPage.toString()
      });

      if (tabValue === 0) {
        params.append('archived', 'false');
      } else if (tabValue === 1) {
        params.append('archived', 'true');
      }

      const response = await apiClient.get(`/documents?${params}`);
      setDocuments(response.data.content || []);
      setTotalCount(response.data.totalElements || 0);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load documents');
    } finally {
      setLoading(false);
    }
  }, [page, rowsPerPage, tabValue]);

  // Load statistics
  const loadStatistics = async () => {
    try {
      const response = await apiClient.get('/documents/statistics');
      setStats(response.data);
    } catch (err) {
      console.error('Failed to load statistics:', err);
    }
  };

  // Search documents
  const searchDocuments = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await apiClient.post('/documents/search', {
        query: searchState.query,
        fileTypes: searchState.fileTypes.length > 0 ? searchState.fileTypes : null,
        author: searchState.author || null,
        tags: searchState.tags ? searchState.tags.split(',').map(t => t.trim()) : null,
        archived: searchState.archived,
        storageTier: searchState.storageTier || null,
        from: page * rowsPerPage,
        size: rowsPerPage
      });

      setDocuments(response.data.documents || []);
      setTotalCount(response.data.totalHits || 0);
      setTabValue(2); // Switch to search results tab
    } catch (err: any) {
      setError(err.response?.data?.message || 'Search failed');
    } finally {
      setLoading(false);
    }
  };

  // Upload document
  const handleUpload = async () => {
    if (!uploadDialog.file) {
      setError('Please select a file');
      return;
    }

    setLoading(true);
    setError(null);

    const formData = new FormData();
    formData.append('file', uploadDialog.file);
    formData.append('title', uploadDialog.title || uploadDialog.file.name);
    formData.append('description', uploadDialog.description);
    formData.append('tags', uploadDialog.tags);
    formData.append('archive', uploadDialog.archive.toString());
    formData.append('compress', uploadDialog.compress.toString());
    formData.append('encrypt', uploadDialog.encrypt.toString());

    try {
      await apiClient.post('/documents/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });

      setSuccess('Document uploaded successfully');
      setUploadDialog({ ...uploadDialog, open: false, file: null, title: '', description: '', tags: '' });
      await loadDocuments();
      await loadStatistics();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Upload failed');
    } finally {
      setLoading(false);
    }
  };

  // Archive documents
  const handleArchive = async () => {
    setLoading(true);
    setError(null);

    try {
      await apiClient.post('/documents/archive', {
        documentIds: archiveDialog.documentIds,
        archiveType: archiveDialog.archiveType,
        compress: archiveDialog.compress,
        encrypt: archiveDialog.encrypt,
        archiveReason: archiveDialog.reason,
        requestedBy: 'admin' // TODO: Get from auth context
      });

      setSuccess(`${archiveDialog.documentIds.length} document(s) archived successfully`);
      setArchiveDialog({ ...archiveDialog, open: false, documentIds: [] });
      setSelectedDocuments([]);
      await loadDocuments();
      await loadStatistics();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Archive failed');
    } finally {
      setLoading(false);
    }
  };

  // Restore document
  const handleRestore = async (documentId: string) => {
    setLoading(true);
    setError(null);

    try {
      await apiClient.post(`/documents/${documentId}/restore`);
      setSuccess('Document restored successfully');
      await loadDocuments();
      await loadStatistics();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Restore failed');
    } finally {
      setLoading(false);
    }
  };

  // Download document
  const handleDownload = async (documentId: string, filename: string) => {
    try {
      const response = await apiClient.get(`/documents/${documentId}/download`, {
        responseType: 'blob'
      });

      const blob = new Blob([response.data]);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      link.click();
      window.URL.revokeObjectURL(url);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Download failed');
    }
  };

  // Delete document
  const handleDelete = async (documentId: string) => {
    if (!window.confirm('Are you sure you want to delete this document?')) {
      return;
    }

    setLoading(true);
    setError(null);

    try {
      await apiClient.delete(`/documents/${documentId}`);
      setSuccess('Document deleted successfully');
      await loadDocuments();
      await loadStatistics();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Delete failed');
    } finally {
      setLoading(false);
    }
  };

  // Format file size
  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
  };

  // Format date
  const formatDate = (dateString: string) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleString();
  };

  // Get storage tier chip color
  const getStorageTierColor = (tier?: string) => {
    switch (tier) {
      case 'HOT': return 'error';
      case 'WARM': return 'warning';
      case 'COLD': return 'info';
      case 'GLACIER': return 'default';
      default: return 'primary';
    }
  };

  useEffect(() => {
    loadDocuments();
    loadStatistics();
  }, [loadDocuments]);

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box>
          <Typography variant="h4" fontWeight="bold">
            Document Management
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Upload, archive, and manage documents with advanced search capabilities
          </Typography>
        </Box>
        <Box display="flex" gap={2}>
          <Button
            variant="outlined"
            startIcon={<Refresh />}
            onClick={() => { loadDocuments(); loadStatistics(); }}
            disabled={loading}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            startIcon={<CloudUpload />}
            onClick={() => setUploadDialog({ ...uploadDialog, open: true })}
            disabled={loading}
          >
            Upload Document
          </Button>
        </Box>
      </Box>

      {/* Statistics Cards */}
      <Grid container spacing={2} mb={3}>
        <Grid item xs={12} sm={3}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" gutterBottom>
                Total Documents
              </Typography>
              <Typography variant="h5" fontWeight="bold">
                {stats.totalDocuments}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={3}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" gutterBottom>
                Active Documents
              </Typography>
              <Typography variant="h5" fontWeight="bold" color="success.main">
                {stats.activeDocuments}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={3}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" gutterBottom>
                Archived Documents
              </Typography>
              <Typography variant="h5" fontWeight="bold" color="info.main">
                {stats.archivedDocuments}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={3}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" gutterBottom>
                Total Size
              </Typography>
              <Typography variant="h5" fontWeight="bold">
                {formatFileSize(stats.totalSize)}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Search Bar */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={4}>
            <TextField
              fullWidth
              placeholder="Search documents..."
              value={searchState.query}
              onChange={(e) => setSearchState({ ...searchState, query: e.target.value })}
              onKeyPress={(e) => e.key === 'Enter' && searchDocuments()}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Search />
                  </InputAdornment>
                )
              }}
            />
          </Grid>
          <Grid item xs={12} sm={2}>
            <TextField
              fullWidth
              select
              label="File Type"
              value={searchState.fileTypes.join(',')}
              onChange={(e) => setSearchState({
                ...searchState,
                fileTypes: e.target.value ? e.target.value.split(',') : []
              })}
            >
              <MenuItem value="">All</MenuItem>
              <MenuItem value="pdf">PDF</MenuItem>
              <MenuItem value="doc,docx">Word</MenuItem>
              <MenuItem value="txt">Text</MenuItem>
              <MenuItem value="xls,xlsx">Excel</MenuItem>
            </TextField>
          </Grid>
          <Grid item xs={12} sm={2}>
            <FormControl fullWidth>
              <InputLabel>Storage Tier</InputLabel>
              <Select
                value={searchState.storageTier}
                onChange={(e) => setSearchState({ ...searchState, storageTier: e.target.value as string })}
                label="Storage Tier"
              >
                <MenuItem value="">All</MenuItem>
                <MenuItem value="HOT">Hot</MenuItem>
                <MenuItem value="WARM">Warm</MenuItem>
                <MenuItem value="COLD">Cold</MenuItem>
                <MenuItem value="GLACIER">Glacier</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={2}>
            <TextField
              fullWidth
              label="Tags"
              placeholder="tag1, tag2"
              value={searchState.tags}
              onChange={(e) => setSearchState({ ...searchState, tags: e.target.value })}
            />
          </Grid>
          <Grid item xs={12} sm={2}>
            <Button
              fullWidth
              variant="contained"
              onClick={searchDocuments}
              disabled={loading}
              sx={{ height: 56 }}
            >
              Search
            </Button>
          </Grid>
        </Grid>
      </Paper>

      {/* Alerts */}
      {error && (
        <Alert severity="error" onClose={() => setError(null)} sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      {success && (
        <Alert severity="success" onClose={() => setSuccess(null)} sx={{ mb: 2 }}>
          {success}
        </Alert>
      )}

      {/* Documents Table */}
      <Paper>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={(e, newValue) => { setTabValue(newValue); setPage(0); }}>
            <Tab label="Active Documents" />
            <Tab label="Archived Documents" />
            {searchState.query && <Tab label="Search Results" />}
          </Tabs>
        </Box>

        {/* Action bar for selected documents */}
        {selectedDocuments.length > 0 && (
          <Box sx={{ p: 2, bgcolor: 'action.hover' }}>
            <Typography variant="body2" sx={{ mb: 1 }}>
              {selectedDocuments.length} document(s) selected
            </Typography>
            <Box display="flex" gap={1}>
              <Button
                size="small"
                variant="outlined"
                startIcon={<Archive />}
                onClick={() => setArchiveDialog({
                  ...archiveDialog,
                  open: true,
                  documentIds: selectedDocuments
                })}
              >
                Archive Selected
              </Button>
              <Button
                size="small"
                variant="outlined"
                color="error"
                startIcon={<Delete />}
                onClick={() => {
                  if (window.confirm(`Delete ${selectedDocuments.length} document(s)?`)) {
                    // TODO: Implement batch delete
                  }
                }}
              >
                Delete Selected
              </Button>
            </Box>
          </Box>
        )}

        {loading && <LinearProgress />}

        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell padding="checkbox">
                  <Checkbox
                    indeterminate={selectedDocuments.length > 0 && selectedDocuments.length < documents.length}
                    checked={documents.length > 0 && selectedDocuments.length === documents.length}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setSelectedDocuments(documents.map(d => d.id));
                      } else {
                        setSelectedDocuments([]);
                      }
                    }}
                  />
                </TableCell>
                <TableCell>Filename</TableCell>
                <TableCell>Title</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Size</TableCell>
                <TableCell>Storage</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Created</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {documents.map((document) => (
                <TableRow key={document.id} hover>
                  <TableCell padding="checkbox">
                    <Checkbox
                      checked={selectedDocuments.includes(document.id)}
                      onChange={(e) => {
                        if (e.target.checked) {
                          setSelectedDocuments([...selectedDocuments, document.id]);
                        } else {
                          setSelectedDocuments(selectedDocuments.filter(id => id !== document.id));
                        }
                      }}
                    />
                  </TableCell>
                  <TableCell>
                    <Box display="flex" alignItems="center" gap={1}>
                      {document.filename}
                      {document.compressed && (
                        <Tooltip title={`Compression ratio: ${(document.compressionRatio || 0).toFixed(2)}`}>
                          <Chip label="C" size="small" color="info" />
                        </Tooltip>
                      )}
                      {document.encrypted && (
                        <Tooltip title="Encrypted">
                          <Chip label="E" size="small" color="warning" />
                        </Tooltip>
                      )}
                    </Box>
                  </TableCell>
                  <TableCell>{document.title || document.filename}</TableCell>
                  <TableCell>
                    <Chip label={document.fileType.toUpperCase()} size="small" />
                  </TableCell>
                  <TableCell>{formatFileSize(document.size)}</TableCell>
                  <TableCell>
                    {document.storageTier && (
                      <Chip
                        label={document.storageTier}
                        size="small"
                        color={getStorageTierColor(document.storageTier)}
                      />
                    )}
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={document.archived ? 'ARCHIVED' : document.status}
                      size="small"
                      color={document.archived ? 'info' : 'success'}
                    />
                  </TableCell>
                  <TableCell>{formatDate(document.createdDate)}</TableCell>
                  <TableCell>
                    <Box display="flex" gap={1}>
                      <Tooltip title="Download">
                        <IconButton
                          size="small"
                          onClick={() => handleDownload(document.id, document.filename)}
                        >
                          <Download />
                        </IconButton>
                      </Tooltip>
                      {document.archived ? (
                        <Tooltip title="Restore">
                          <IconButton
                            size="small"
                            onClick={() => handleRestore(document.id)}
                          >
                            <Restore />
                          </IconButton>
                        </Tooltip>
                      ) : (
                        <Tooltip title="Archive">
                          <IconButton
                            size="small"
                            onClick={() => setArchiveDialog({
                              ...archiveDialog,
                              open: true,
                              documentIds: [document.id]
                            })}
                          >
                            <Archive />
                          </IconButton>
                        </Tooltip>
                      )}
                      <Tooltip title="Delete">
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => handleDelete(document.id)}
                        >
                          <Delete />
                        </IconButton>
                      </Tooltip>
                    </Box>
                  </TableCell>
                </TableRow>
              ))}

              {documents.length === 0 && (
                <TableRow>
                  <TableCell colSpan={9} align="center" sx={{ py: 3 }}>
                    <Typography variant="body2" color="text.secondary">
                      No documents found
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>

        <TablePagination
          component="div"
          count={totalCount}
          page={page}
          onPageChange={(e, newPage) => setPage(newPage)}
          rowsPerPage={rowsPerPage}
          onRowsPerPageChange={(e) => {
            setRowsPerPage(parseInt(e.target.value, 10));
            setPage(0);
          }}
          rowsPerPageOptions={[10, 20, 50]}
        />
      </Paper>

      {/* Upload Dialog */}
      <Dialog open={uploadDialog.open} onClose={() => setUploadDialog({ ...uploadDialog, open: false })} maxWidth="sm" fullWidth>
        <DialogTitle>Upload Document</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <Button
              variant="outlined"
              component="label"
              fullWidth
              startIcon={<CloudUpload />}
              sx={{ py: 2 }}
            >
              {uploadDialog.file ? uploadDialog.file.name : 'Select File'}
              <input
                type="file"
                hidden
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file) {
                    setUploadDialog({
                      ...uploadDialog,
                      file,
                      title: uploadDialog.title || file.name
                    });
                  }
                }}
              />
            </Button>

            <TextField
              fullWidth
              label="Title"
              value={uploadDialog.title}
              onChange={(e) => setUploadDialog({ ...uploadDialog, title: e.target.value })}
            />

            <TextField
              fullWidth
              multiline
              rows={3}
              label="Description"
              value={uploadDialog.description}
              onChange={(e) => setUploadDialog({ ...uploadDialog, description: e.target.value })}
            />

            <TextField
              fullWidth
              label="Tags (comma-separated)"
              placeholder="tag1, tag2, tag3"
              value={uploadDialog.tags}
              onChange={(e) => setUploadDialog({ ...uploadDialog, tags: e.target.value })}
            />

            <Divider />

            <FormControlLabel
              control={
                <Checkbox
                  checked={uploadDialog.archive}
                  onChange={(e) => setUploadDialog({ ...uploadDialog, archive: e.target.checked })}
                />
              }
              label="Archive immediately after upload"
            />

            <FormControlLabel
              control={
                <Checkbox
                  checked={uploadDialog.compress}
                  onChange={(e) => setUploadDialog({ ...uploadDialog, compress: e.target.checked })}
                />
              }
              label="Compress document"
            />

            <FormControlLabel
              control={
                <Checkbox
                  checked={uploadDialog.encrypt}
                  onChange={(e) => setUploadDialog({ ...uploadDialog, encrypt: e.target.checked })}
                />
              }
              label="Encrypt document"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setUploadDialog({ ...uploadDialog, open: false })}>
            Cancel
          </Button>
          <Button onClick={handleUpload} variant="contained" disabled={!uploadDialog.file || loading}>
            Upload
          </Button>
        </DialogActions>
      </Dialog>

      {/* Archive Dialog */}
      <Dialog open={archiveDialog.open} onClose={() => setArchiveDialog({ ...archiveDialog, open: false })} maxWidth="sm" fullWidth>
        <DialogTitle>Archive Documents</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <Typography variant="body2" color="text.secondary">
              Archiving {archiveDialog.documentIds.length} document(s)
            </Typography>

            <FormControl fullWidth>
              <InputLabel>Archive Tier</InputLabel>
              <Select
                value={archiveDialog.archiveType}
                onChange={(e) => setArchiveDialog({ ...archiveDialog, archiveType: e.target.value as string })}
                label="Archive Tier"
              >
                <MenuItem value="HOT">Hot - Frequently accessed</MenuItem>
                <MenuItem value="WARM">Warm - Occasionally accessed</MenuItem>
                <MenuItem value="COLD">Cold - Rarely accessed</MenuItem>
                <MenuItem value="GLACIER">Glacier - Long-term storage</MenuItem>
              </Select>
            </FormControl>

            <TextField
              fullWidth
              multiline
              rows={3}
              label="Archive Reason"
              value={archiveDialog.reason}
              onChange={(e) => setArchiveDialog({ ...archiveDialog, reason: e.target.value })}
              helperText="Provide a reason for archiving these documents"
            />

            <FormControlLabel
              control={
                <Checkbox
                  checked={archiveDialog.compress}
                  onChange={(e) => setArchiveDialog({ ...archiveDialog, compress: e.target.checked })}
                />
              }
              label="Compress documents (reduces storage space)"
            />

            <FormControlLabel
              control={
                <Checkbox
                  checked={archiveDialog.encrypt}
                  onChange={(e) => setArchiveDialog({ ...archiveDialog, encrypt: e.target.checked })}
                />
              }
              label="Encrypt documents (enhanced security)"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setArchiveDialog({ ...archiveDialog, open: false })}>
            Cancel
          </Button>
          <Button onClick={handleArchive} variant="contained" disabled={loading}>
            Archive
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Documents;