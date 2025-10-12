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
  Tabs,
  Tab,
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
  Delete as DeleteIcon,
  Refresh as RefreshIcon,
  Visibility as ViewIcon,
  Download as DownloadIcon,
  CheckCircle as ApproveIcon,
  Cancel as RejectIcon,
  Block as RevokeIcon,
} from '@mui/icons-material';
import complianceService, {
  DataSubjectRequest,
  ConsentRecord,
  RetentionPolicy,
  AuditLog,
  ComplianceDashboard,
  DataSubjectRequestConfig,
  ConsentConfig,
  RetentionPolicyConfig,
} from '../services/complianceService';

const Compliance: React.FC = () => {
  const [dashboard, setDashboard] = useState<ComplianceDashboard | null>(null);
  const [activeTab, setActiveTab] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Requests state
  const [requests, setRequests] = useState<DataSubjectRequest[]>([]);
  const [requestsPage, setRequestsPage] = useState(0);
  const [requestsRowsPerPage, setRequestsRowsPerPage] = useState(20);
  const [requestsTotalElements, setRequestsTotalElements] = useState(0);
  const [requestsStatusFilter, setRequestsStatusFilter] = useState<string>('');
  const [requestsTypeFilter, setRequestsTypeFilter] = useState<string>('');
  const [createRequestDialogOpen, setCreateRequestDialogOpen] = useState(false);
  const [newRequest, setNewRequest] = useState<Partial<DataSubjectRequestConfig>>({
    requestType: 'ACCESS',
    regulation: 'GDPR',
    dataSubjectId: '',
    dataSubjectEmail: '',
    requestDetails: '',
    priority: 'MEDIUM',
  });

  // Consents state
  const [consents, setConsents] = useState<ConsentRecord[]>([]);
  const [consentsPage, setConsentsPage] = useState(0);
  const [consentsRowsPerPage, setConsentsRowsPerPage] = useState(20);
  const [consentsTotalElements, setConsentsTotalElements] = useState(0);
  const [createConsentDialogOpen, setCreateConsentDialogOpen] = useState(false);

  // Retention Policies state
  const [policies, setPolicies] = useState<RetentionPolicy[]>([]);
  const [policiesPage, setPoliciesPage] = useState(0);
  const [policiesRowsPerPage, setPoliciesRowsPerPage] = useState(20);
  const [policiesTotalElements, setPoliciesTotalElements] = useState(0);
  const [createPolicyDialogOpen, setCreatePolicyDialogOpen] = useState(false);
  const [newPolicy, setNewPolicy] = useState<Partial<RetentionPolicyConfig>>({
    name: '',
    description: '',
    dataType: '',
    retentionPeriod: 90,
    retentionUnit: 'DAYS',
    action: 'ARCHIVE',
    enabled: true,
  });

  // Audit Logs state
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [auditLogsPage, setAuditLogsPage] = useState(0);
  const [auditLogsRowsPerPage, setAuditLogsRowsPerPage] = useState(20);
  const [auditLogsTotalElements, setAuditLogsTotalElements] = useState(0);

  useEffect(() => {
    loadDashboard();
  }, []);

  useEffect(() => {
    if (activeTab === 0) loadRequests();
    else if (activeTab === 1) loadConsents();
    else if (activeTab === 2) loadPolicies();
    else if (activeTab === 3) loadAuditLogs();
  }, [activeTab, requestsPage, requestsRowsPerPage, requestsStatusFilter, requestsTypeFilter,
    consentsPage, consentsRowsPerPage, policiesPage, policiesRowsPerPage,
    auditLogsPage, auditLogsRowsPerPage]);

  const loadDashboard = async () => {
    try {
      setLoading(true);
      const data = await complianceService.getDashboard();
      setDashboard(data);
      setError(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load dashboard');
    } finally {
      setLoading(false);
    }
  };

  const loadRequests = async () => {
    try {
      const response = await complianceService.getRequests(
        requestsPage,
        requestsRowsPerPage,
        requestsStatusFilter,
        requestsTypeFilter
      );
      setRequests(response.content || []);
      setRequestsTotalElements(response.totalElements || 0);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load requests');
    }
  };

  const loadConsents = async () => {
    try {
      const response = await complianceService.getConsents(consentsPage, consentsRowsPerPage);
      setConsents(response.content || []);
      setConsentsTotalElements(response.totalElements || 0);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load consents');
    }
  };

  const loadPolicies = async () => {
    try {
      const response = await complianceService.getRetentionPolicies(policiesPage, policiesRowsPerPage);
      setPolicies(response.content || []);
      setPoliciesTotalElements(response.totalElements || 0);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load policies');
    }
  };

  const loadAuditLogs = async () => {
    try {
      const response = await complianceService.getAuditLogs(auditLogsPage, auditLogsRowsPerPage);
      setAuditLogs(response.content || []);
      setAuditLogsTotalElements(response.totalElements || 0);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load audit logs');
    }
  };

  const handleCreateRequest = async () => {
    try {
      await complianceService.createRequest(newRequest as DataSubjectRequestConfig);
      setCreateRequestDialogOpen(false);
      setNewRequest({
        requestType: 'ACCESS',
        regulation: 'GDPR',
        dataSubjectId: '',
        dataSubjectEmail: '',
        requestDetails: '',
        priority: 'MEDIUM',
      });
      loadRequests();
      loadDashboard();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create request');
    }
  };

  const handleProcessRequest = async (id: string) => {
    try {
      await complianceService.processRequest(id);
      loadRequests();
      loadDashboard();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to process request');
    }
  };

  const handleExportRequestData = async (id: string) => {
    try {
      const blob = await complianceService.exportRequestData(id, 'JSON');
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `data-export-${id}.json`;
      a.click();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to export data');
    }
  };

  const handleRevokeConsent = async (id: string) => {
    if (window.confirm('Are you sure you want to revoke this consent?')) {
      try {
        await complianceService.revokeConsent(id);
        loadConsents();
        loadDashboard();
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to revoke consent');
      }
    }
  };

  const handleCreatePolicy = async () => {
    try {
      await complianceService.createRetentionPolicy(newPolicy as RetentionPolicyConfig);
      setCreatePolicyDialogOpen(false);
      setNewPolicy({
        name: '',
        description: '',
        dataType: '',
        retentionPeriod: 90,
        retentionUnit: 'DAYS',
        action: 'ARCHIVE',
        enabled: true,
      });
      loadPolicies();
      loadDashboard();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create policy');
    }
  };

  const handleExecutePolicy = async (id: string) => {
    if (window.confirm('Are you sure you want to execute this retention policy?')) {
      try {
        await complianceService.executeRetentionPolicy(id);
        loadPolicies();
        loadDashboard();
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to execute policy');
      }
    }
  };

  const handleDeletePolicy = async (id: string) => {
    if (window.confirm('Are you sure you want to delete this policy?')) {
      try {
        await complianceService.deleteRetentionPolicy(id);
        loadPolicies();
        loadDashboard();
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to delete policy');
      }
    }
  };

  const getRequestStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return 'success';
      case 'IN_PROGRESS':
        return 'info';
      case 'PENDING':
        return 'warning';
      case 'REJECTED':
        return 'error';
      default:
        return 'default';
    }
  };

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'URGENT':
        return 'error';
      case 'HIGH':
        return 'warning';
      case 'MEDIUM':
        return 'info';
      case 'LOW':
        return 'default';
      default:
        return 'default';
    }
  };

  if (loading && !dashboard) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '400px' }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold" gutterBottom>
            Compliance Management
          </Typography>
          <Typography variant="body2" color="text.secondary">
            GDPR & CCPA compliance monitoring and management
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={loadDashboard}>
            Refresh
          </Button>
          {activeTab === 0 && (
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => setCreateRequestDialogOpen(true)}
            >
              New Request
            </Button>
          )}
          {activeTab === 1 && (
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => setCreateConsentDialogOpen(true)}
            >
              New Consent
            </Button>
          )}
          {activeTab === 2 && (
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => setCreatePolicyDialogOpen(true)}
            >
              New Policy
            </Button>
          )}
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Dashboard Stats */}
      {dashboard && (
        <Grid container spacing={3} sx={{ mb: 3 }}>
          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Typography color="text.secondary" gutterBottom>
                  Compliance Score
                </Typography>
                <Typography variant="h4" color="success.main">
                  {(dashboard.complianceScore || 0).toFixed(1)}%
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  GDPR: {(dashboard.gdprCompliance || 0).toFixed(1)}% | CCPA: {(dashboard.ccpaCompliance || 0).toFixed(1)}%
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Typography color="text.secondary" gutterBottom>
                  Pending Requests
                </Typography>
                <Typography variant="h4" color="warning.main">
                  {dashboard.pendingRequests}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {dashboard.overdueRequests} overdue
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Typography color="text.secondary" gutterBottom>
                  Active Consents
                </Typography>
                <Typography variant="h4">{dashboard.activeConsents}</Typography>
                <Typography variant="body2" color="text.secondary">
                  of {dashboard.totalConsents} total
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Typography color="text.secondary" gutterBottom>
                  Active Policies
                </Typography>
                <Typography variant="h4">{dashboard.activePolicies}</Typography>
                <Typography variant="body2" color="text.secondary">
                  of {dashboard.totalPolicies} total
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* Tabs */}
      <Paper sx={{ mb: 2 }}>
        <Tabs value={activeTab} onChange={(_, newValue) => setActiveTab(newValue)}>
          <Tab label="Data Subject Requests" />
          <Tab label="Consents" />
          <Tab label="Retention Policies" />
          <Tab label="Audit Logs" />
        </Tabs>
      </Paper>

      {/* Requests Tab */}
      {activeTab === 0 && (
        <>
          <Box sx={{ mb: 2, display: 'flex', gap: 2 }}>
            <FormControl sx={{ minWidth: 200 }}>
              <InputLabel>Filter by Status</InputLabel>
              <Select
                value={requestsStatusFilter}
                label="Filter by Status"
                onChange={(e) => setRequestsStatusFilter(e.target.value)}
              >
                <MenuItem value="">All</MenuItem>
                <MenuItem value="PENDING">Pending</MenuItem>
                <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
                <MenuItem value="COMPLETED">Completed</MenuItem>
                <MenuItem value="REJECTED">Rejected</MenuItem>
              </Select>
            </FormControl>
            <FormControl sx={{ minWidth: 200 }}>
              <InputLabel>Filter by Type</InputLabel>
              <Select
                value={requestsTypeFilter}
                label="Filter by Type"
                onChange={(e) => setRequestsTypeFilter(e.target.value)}
              >
                <MenuItem value="">All</MenuItem>
                <MenuItem value="ACCESS">Access</MenuItem>
                <MenuItem value="ERASURE">Erasure</MenuItem>
                <MenuItem value="RECTIFICATION">Rectification</MenuItem>
                <MenuItem value="PORTABILITY">Portability</MenuItem>
              </Select>
            </FormControl>
          </Box>

          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Type</TableCell>
                  <TableCell>Regulation</TableCell>
                  <TableCell>Data Subject</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Priority</TableCell>
                  <TableCell>Due Date</TableCell>
                  <TableCell>Requested At</TableCell>
                  <TableCell align="center">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {requests.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} align="center">
                      <Typography variant="body2" color="text.secondary">
                        No requests found
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  requests.map((request) => (
                    <TableRow key={request.id} hover>
                      <TableCell>{request.requestType}</TableCell>
                      <TableCell>
                        <Chip label={request.regulation} size="small" />
                      </TableCell>
                      <TableCell>{request.dataSubjectEmail}</TableCell>
                      <TableCell>
                        <Chip
                          label={request.status}
                          color={getRequestStatusColor(request.status) as any}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={request.priority}
                          color={getPriorityColor(request.priority) as any}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>{new Date(request.dueDate).toLocaleDateString()}</TableCell>
                      <TableCell>{new Date(request.requestedAt).toLocaleString()}</TableCell>
                      <TableCell align="center">
                        <Tooltip title="View Details">
                          <IconButton size="small">
                            <ViewIcon />
                          </IconButton>
                        </Tooltip>
                        {request.status === 'PENDING' && (
                          <Tooltip title="Process">
                            <IconButton
                              size="small"
                              color="primary"
                              onClick={() => handleProcessRequest(request.id)}
                            >
                              <ApproveIcon />
                            </IconButton>
                          </Tooltip>
                        )}
                        {request.requestType === 'ACCESS' && request.status === 'COMPLETED' && (
                          <Tooltip title="Export Data">
                            <IconButton
                              size="small"
                              color="success"
                              onClick={() => handleExportRequestData(request.id)}
                            >
                              <DownloadIcon />
                            </IconButton>
                          </Tooltip>
                        )}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
            <TablePagination
              component="div"
              count={requestsTotalElements}
              page={requestsPage}
              onPageChange={(_, newPage) => setRequestsPage(newPage)}
              rowsPerPage={requestsRowsPerPage}
              onRowsPerPageChange={(e) => {
                setRequestsRowsPerPage(parseInt(e.target.value, 10));
                setRequestsPage(0);
              }}
            />
          </TableContainer>
        </>
      )}

      {/* Consents Tab */}
      {activeTab === 1 && (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Data Subject</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Purpose</TableCell>
                <TableCell>Legal Basis</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Granted At</TableCell>
                <TableCell>Expires At</TableCell>
                <TableCell align="center">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {consents.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} align="center">
                    <Typography variant="body2" color="text.secondary">
                      No consents found
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                consents.map((consent) => (
                  <TableRow key={consent.id} hover>
                    <TableCell>{consent.dataSubjectEmail}</TableCell>
                    <TableCell>{consent.consentType}</TableCell>
                    <TableCell>{consent.purpose}</TableCell>
                    <TableCell>{consent.legalBasis}</TableCell>
                    <TableCell>
                      <Chip
                        label={consent.granted ? 'Active' : 'Revoked'}
                        color={consent.granted ? 'success' : 'default'}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>
                      {consent.grantedAt ? new Date(consent.grantedAt).toLocaleString() : '-'}
                    </TableCell>
                    <TableCell>
                      {consent.expiresAt ? new Date(consent.expiresAt).toLocaleDateString() : 'Never'}
                    </TableCell>
                    <TableCell align="center">
                      <Tooltip title="View Details">
                        <IconButton size="small">
                          <ViewIcon />
                        </IconButton>
                      </Tooltip>
                      {consent.granted && (
                        <Tooltip title="Revoke">
                          <IconButton
                            size="small"
                            color="error"
                            onClick={() => handleRevokeConsent(consent.id)}
                          >
                            <RevokeIcon />
                          </IconButton>
                        </Tooltip>
                      )}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
          <TablePagination
            component="div"
            count={consentsTotalElements}
            page={consentsPage}
            onPageChange={(_, newPage) => setConsentsPage(newPage)}
            rowsPerPage={consentsRowsPerPage}
            onRowsPerPageChange={(e) => {
              setConsentsRowsPerPage(parseInt(e.target.value, 10));
              setConsentsPage(0);
            }}
          />
        </TableContainer>
      )}

      {/* Retention Policies Tab */}
      {activeTab === 2 && (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Data Type</TableCell>
                <TableCell>Retention Period</TableCell>
                <TableCell>Action</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Last Executed</TableCell>
                <TableCell align="center">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {policies.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center">
                    <Typography variant="body2" color="text.secondary">
                      No policies found
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                policies.map((policy) => (
                  <TableRow key={policy.id} hover>
                    <TableCell>{policy.name}</TableCell>
                    <TableCell>{policy.dataType}</TableCell>
                    <TableCell>
                      {policy.retentionPeriod} {policy.retentionUnit}
                    </TableCell>
                    <TableCell>{policy.action}</TableCell>
                    <TableCell>
                      <Chip
                        label={policy.enabled ? 'Active' : 'Disabled'}
                        color={policy.enabled ? 'success' : 'default'}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>
                      {policy.lastExecutedAt
                        ? new Date(policy.lastExecutedAt).toLocaleString()
                        : 'Never'}
                    </TableCell>
                    <TableCell align="center">
                      <Tooltip title="View Details">
                        <IconButton size="small">
                          <ViewIcon />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Execute">
                        <IconButton
                          size="small"
                          color="primary"
                          onClick={() => handleExecutePolicy(policy.id)}
                        >
                          <PlayIcon />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Delete">
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => handleDeletePolicy(policy.id)}
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
            count={policiesTotalElements}
            page={policiesPage}
            onPageChange={(_, newPage) => setPoliciesPage(newPage)}
            rowsPerPage={policiesRowsPerPage}
            onRowsPerPageChange={(e) => {
              setPoliciesRowsPerPage(parseInt(e.target.value, 10));
              setPoliciesPage(0);
            }}
          />
        </TableContainer>
      )}

      {/* Audit Logs Tab */}
      {activeTab === 3 && (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>User</TableCell>
                <TableCell>Action</TableCell>
                <TableCell>Entity Type</TableCell>
                <TableCell>Details</TableCell>
                <TableCell>IP Address</TableCell>
                <TableCell>Timestamp</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {auditLogs.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    <Typography variant="body2" color="text.secondary">
                      No audit logs found
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                auditLogs.map((log) => (
                  <TableRow key={log.id} hover>
                    <TableCell>{log.username}</TableCell>
                    <TableCell>{log.action}</TableCell>
                    <TableCell>{log.entityType}</TableCell>
                    <TableCell>{log.details}</TableCell>
                    <TableCell>{log.ipAddress}</TableCell>
                    <TableCell>{new Date(log.timestamp).toLocaleString()}</TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
          <TablePagination
            component="div"
            count={auditLogsTotalElements}
            page={auditLogsPage}
            onPageChange={(_, newPage) => setAuditLogsPage(newPage)}
            rowsPerPage={auditLogsRowsPerPage}
            onRowsPerPageChange={(e) => {
              setAuditLogsRowsPerPage(parseInt(e.target.value, 10));
              setAuditLogsPage(0);
            }}
          />
        </TableContainer>
      )}

      {/* Create Request Dialog */}
      <Dialog
        open={createRequestDialogOpen}
        onClose={() => setCreateRequestDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Create Data Subject Request</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
            <FormControl fullWidth>
              <InputLabel>Request Type</InputLabel>
              <Select
                value={newRequest.requestType}
                label="Request Type"
                onChange={(e) => setNewRequest({ ...newRequest, requestType: e.target.value as any })}
              >
                <MenuItem value="ACCESS">Access (Article 15)</MenuItem>
                <MenuItem value="ERASURE">Erasure (Article 17)</MenuItem>
                <MenuItem value="RECTIFICATION">Rectification (Article 16)</MenuItem>
                <MenuItem value="PORTABILITY">Portability (Article 20)</MenuItem>
                <MenuItem value="RESTRICTION">Restriction (Article 18)</MenuItem>
                <MenuItem value="OBJECTION">Objection (Article 21)</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Regulation</InputLabel>
              <Select
                value={newRequest.regulation}
                label="Regulation"
                onChange={(e) => setNewRequest({ ...newRequest, regulation: e.target.value as any })}
              >
                <MenuItem value="GDPR">GDPR</MenuItem>
                <MenuItem value="CCPA">CCPA</MenuItem>
              </Select>
            </FormControl>
            <TextField
              label="Data Subject ID"
              fullWidth
              value={newRequest.dataSubjectId}
              onChange={(e) => setNewRequest({ ...newRequest, dataSubjectId: e.target.value })}
            />
            <TextField
              label="Data Subject Email"
              fullWidth
              type="email"
              value={newRequest.dataSubjectEmail}
              onChange={(e) => setNewRequest({ ...newRequest, dataSubjectEmail: e.target.value })}
            />
            <TextField
              label="Request Details"
              fullWidth
              multiline
              rows={4}
              value={newRequest.requestDetails}
              onChange={(e) => setNewRequest({ ...newRequest, requestDetails: e.target.value })}
            />
            <FormControl fullWidth>
              <InputLabel>Priority</InputLabel>
              <Select
                value={newRequest.priority}
                label="Priority"
                onChange={(e) => setNewRequest({ ...newRequest, priority: e.target.value as any })}
              >
                <MenuItem value="LOW">Low</MenuItem>
                <MenuItem value="MEDIUM">Medium</MenuItem>
                <MenuItem value="HIGH">High</MenuItem>
                <MenuItem value="URGENT">Urgent</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateRequestDialogOpen(false)}>Cancel</Button>
          <Button
            onClick={handleCreateRequest}
            variant="contained"
            disabled={!newRequest.dataSubjectId || !newRequest.dataSubjectEmail || !newRequest.requestDetails}
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>

      {/* Create Policy Dialog */}
      <Dialog
        open={createPolicyDialogOpen}
        onClose={() => setCreatePolicyDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Create Retention Policy</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField
              label="Name"
              fullWidth
              value={newPolicy.name}
              onChange={(e) => setNewPolicy({ ...newPolicy, name: e.target.value })}
            />
            <TextField
              label="Description"
              fullWidth
              multiline
              rows={3}
              value={newPolicy.description}
              onChange={(e) => setNewPolicy({ ...newPolicy, description: e.target.value })}
            />
            <TextField
              label="Data Type"
              fullWidth
              value={newPolicy.dataType}
              onChange={(e) => setNewPolicy({ ...newPolicy, dataType: e.target.value })}
            />
            <TextField
              label="Retention Period"
              fullWidth
              type="number"
              value={newPolicy.retentionPeriod}
              onChange={(e) => setNewPolicy({ ...newPolicy, retentionPeriod: parseInt(e.target.value) })}
            />
            <FormControl fullWidth>
              <InputLabel>Retention Unit</InputLabel>
              <Select
                value={newPolicy.retentionUnit}
                label="Retention Unit"
                onChange={(e) => setNewPolicy({ ...newPolicy, retentionUnit: e.target.value as any })}
              >
                <MenuItem value="DAYS">Days</MenuItem>
                <MenuItem value="MONTHS">Months</MenuItem>
                <MenuItem value="YEARS">Years</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Action</InputLabel>
              <Select
                value={newPolicy.action}
                label="Action"
                onChange={(e) => setNewPolicy({ ...newPolicy, action: e.target.value as any })}
              >
                <MenuItem value="DELETE">Delete</MenuItem>
                <MenuItem value="ARCHIVE">Archive</MenuItem>
                <MenuItem value="COLD_STORAGE">Cold Storage</MenuItem>
                <MenuItem value="ANONYMIZE">Anonymize</MenuItem>
                <MenuItem value="SOFT_DELETE">Soft Delete</MenuItem>
                <MenuItem value="NOTIFY">Notify Only</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreatePolicyDialogOpen(false)}>Cancel</Button>
          <Button
            onClick={handleCreatePolicy}
            variant="contained"
            disabled={!newPolicy.name || !newPolicy.dataType}
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Compliance;
