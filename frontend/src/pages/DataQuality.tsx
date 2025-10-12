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
  LinearProgress,
} from '@mui/material';
import {
  Add as AddIcon,
  PlayArrow as PlayIcon,
  Delete as DeleteIcon,
  Refresh as RefreshIcon,
  Visibility as ViewIcon,
  Edit as EditIcon,
  CheckCircle as ResolveIcon,
  Assessment as ProfileIcon,
} from '@mui/icons-material';
import dataQualityService, {
  DataQualityRule,
  DataQualityIssue,
  DataQualityProfile,
  DataQualityDashboard,
  RuleConfig,
} from '../services/dataQualityService';

const DataQuality: React.FC = () => {
  const [dashboard, setDashboard] = useState<DataQualityDashboard | null>(null);
  const [activeTab, setActiveTab] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Rules state
  const [rules, setRules] = useState<DataQualityRule[]>([]);
  const [rulesPage, setRulesPage] = useState(0);
  const [rulesRowsPerPage, setRulesRowsPerPage] = useState(20);
  const [rulesTotalElements, setRulesTotalElements] = useState(0);
  const [rulesDimensionFilter, setRulesDimensionFilter] = useState<string>('');
  const [rulesSeverityFilter, setRulesSeverityFilter] = useState<string>('');
  const [createRuleDialogOpen, setCreateRuleDialogOpen] = useState(false);
  const [newRule, setNewRule] = useState<Partial<RuleConfig>>({
    name: '',
    description: '',
    ruleType: 'NULL_CHECK',
    dimension: 'COMPLETENESS',
    severity: 'MEDIUM',
    enabled: true,
    parameters: {},
  });

  // Issues state
  const [issues, setIssues] = useState<DataQualityIssue[]>([]);
  const [issuesPage, setIssuesPage] = useState(0);
  const [issuesRowsPerPage, setIssuesRowsPerPage] = useState(20);
  const [issuesTotalElements, setIssuesTotalElements] = useState(0);
  const [issuesStatusFilter, setIssuesStatusFilter] = useState<string>('');
  const [issuesSeverityFilter, setIssuesSeverityFilter] = useState<string>('');

  // Profiles state
  const [profiles, setProfiles] = useState<DataQualityProfile[]>([]);
  const [profilesPage, setProfilesPage] = useState(0);
  const [profilesRowsPerPage, setProfilesRowsPerPage] = useState(20);
  const [profilesTotalElements, setProfilesTotalElements] = useState(0);
  const [profileDialogOpen, setProfileDialogOpen] = useState(false);

  useEffect(() => {
    loadDashboard();
  }, []);

  useEffect(() => {
    if (activeTab === 0) loadRules();
    else if (activeTab === 1) loadIssues();
    else if (activeTab === 2) loadProfiles();
  }, [activeTab, rulesPage, rulesRowsPerPage, rulesDimensionFilter, rulesSeverityFilter,
    issuesPage, issuesRowsPerPage, issuesStatusFilter, issuesSeverityFilter,
    profilesPage, profilesRowsPerPage]);

  const loadDashboard = async () => {
    try {
      setLoading(true);
      const data = await dataQualityService.getDashboard();
      setDashboard(data);
      setError(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load dashboard');
    } finally {
      setLoading(false);
    }
  };

  const loadRules = async () => {
    try {
      const response = await dataQualityService.getRules(
        rulesPage,
        rulesRowsPerPage,
        rulesDimensionFilter,
        rulesSeverityFilter
      );
      setRules(response.content || []);
      setRulesTotalElements(response.totalElements || 0);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load rules');
    }
  };

  const loadIssues = async () => {
    try {
      const response = await dataQualityService.getIssues(
        issuesPage,
        issuesRowsPerPage,
        issuesStatusFilter,
        issuesSeverityFilter
      );
      setIssues(response.content || []);
      setIssuesTotalElements(response.totalElements || 0);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load issues');
    }
  };

  const loadProfiles = async () => {
    try {
      const response = await dataQualityService.getProfiles(profilesPage, profilesRowsPerPage);
      setProfiles(response.content || []);
      setProfilesTotalElements(response.totalElements || 0);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load profiles');
    }
  };

  const handleCreateRule = async () => {
    try {
      await dataQualityService.createRule(newRule as RuleConfig);
      setCreateRuleDialogOpen(false);
      setNewRule({
        name: '',
        description: '',
        ruleType: 'NULL_CHECK',
        dimension: 'COMPLETENESS',
        severity: 'MEDIUM',
        enabled: true,
        parameters: {},
      });
      loadRules();
      loadDashboard();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create rule');
    }
  };

  const handleExecuteRule = async (id: string) => {
    try {
      await dataQualityService.executeRule(id, {});
      loadIssues();
      loadDashboard();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to execute rule');
    }
  };

  const handleDeleteRule = async (id: string) => {
    if (window.confirm('Are you sure you want to delete this rule?')) {
      try {
        await dataQualityService.deleteRule(id);
        loadRules();
        loadDashboard();
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to delete rule');
      }
    }
  };

  const handleResolveIssue = async (id: string) => {
    try {
      await dataQualityService.resolveIssue(id, 'Manually resolved');
      loadIssues();
      loadDashboard();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to resolve issue');
    }
  };

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'CRITICAL':
        return 'error';
      case 'HIGH':
        return 'error';
      case 'MEDIUM':
        return 'warning';
      case 'LOW':
        return 'info';
      default:
        return 'default';
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'RESOLVED':
        return 'success';
      case 'IN_PROGRESS':
        return 'info';
      case 'OPEN':
        return 'warning';
      case 'IGNORED':
        return 'default';
      default:
        return 'default';
    }
  };

  const getScoreColor = (score: number) => {
    if (score >= 90) return 'success.main';
    if (score >= 70) return 'warning.main';
    return 'error.main';
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
            Data Quality
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Monitor and manage data quality rules and issues
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={loadDashboard}>
            Refresh
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setCreateRuleDialogOpen(true)}
          >
            New Rule
          </Button>
          <Button
            variant="contained"
            color="secondary"
            startIcon={<ProfileIcon />}
            onClick={() => setProfileDialogOpen(true)}
          >
            Profile Dataset
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Dashboard Stats */}
      {dashboard && (
        <>
          <Grid container spacing={3} sx={{ mb: 3 }}>
            <Grid item xs={12} sm={6} md={3}>
              <Card>
                <CardContent>
                  <Typography color="text.secondary" gutterBottom>
                    Overall Score
                  </Typography>
                  <Typography variant="h4" sx={{ color: getScoreColor(dashboard.overallScore || 0) }}>
                    {(dashboard.overallScore || 0).toFixed(1)}%
                  </Typography>
                  <LinearProgress
                    variant="determinate"
                    value={dashboard.overallScore || 0}
                    sx={{ mt: 1 }}
                  />
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Card>
                <CardContent>
                  <Typography color="text.secondary" gutterBottom>
                    Active Rules
                  </Typography>
                  <Typography variant="h4">{dashboard.activeRules}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    of {dashboard.totalRules} total
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Card>
                <CardContent>
                  <Typography color="text.secondary" gutterBottom>
                    Open Issues
                  </Typography>
                  <Typography variant="h4" color="warning.main">
                    {dashboard.openIssues}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    of {dashboard.totalIssues} total
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Card>
                <CardContent>
                  <Typography color="text.secondary" gutterBottom>
                    Critical Issues
                  </Typography>
                  <Typography variant="h4" color="error.main">
                    {dashboard.criticalIssues}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Requires immediate attention
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>

          {/* Quality Dimensions */}
          <Grid container spacing={3} sx={{ mb: 3 }}>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Quality Dimensions
                  </Typography>
                  {[
                    { name: 'Completeness', score: dashboard.completenessScore || 0 },
                    { name: 'Accuracy', score: dashboard.accuracyScore || 0 },
                    { name: 'Consistency', score: dashboard.consistencyScore || 0 },
                    { name: 'Validity', score: dashboard.validityScore || 0 },
                    { name: 'Uniqueness', score: dashboard.uniquenessScore || 0 },
                    { name: 'Timeliness', score: dashboard.timelinessScore || 0 },
                  ].map((dimension) => (
                    <Box key={dimension.name} sx={{ mb: 2 }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                        <Typography variant="body2">{dimension.name}</Typography>
                        <Typography
                          variant="body2"
                          sx={{ color: getScoreColor(dimension.score) }}
                        >
                          {dimension.score.toFixed(1)}%
                        </Typography>
                      </Box>
                      <LinearProgress variant="determinate" value={dimension.score} />
                    </Box>
                  ))}
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Issues by Severity
                  </Typography>
                  {dashboard.issuesBySeverity && Object.entries(dashboard.issuesBySeverity).map(([severity, count]) => (
                    <Box
                      key={severity}
                      sx={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        py: 1,
                        borderBottom: '1px solid #e0e0e0',
                      }}
                    >
                      <Box sx={{ display: 'flex', alignItems: 'center' }}>
                        <Chip label={severity} color={getSeverityColor(severity) as any} size="small" sx={{ mr: 1 }} />
                        <Typography variant="body2">{severity} Issues</Typography>
                      </Box>
                      <Typography variant="h6">{count}</Typography>
                    </Box>
                  ))}
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </>
      )}

      {/* Tabs */}
      <Paper sx={{ mb: 2 }}>
        <Tabs value={activeTab} onChange={(_, newValue) => setActiveTab(newValue)}>
          <Tab label="Rules" />
          <Tab label="Issues" />
          <Tab label="Profiles" />
        </Tabs>
      </Paper>

      {/* Rules Tab */}
      {activeTab === 0 && (
        <>
          <Box sx={{ mb: 2, display: 'flex', gap: 2 }}>
            <FormControl sx={{ minWidth: 200 }}>
              <InputLabel>Filter by Dimension</InputLabel>
              <Select
                value={rulesDimensionFilter}
                label="Filter by Dimension"
                onChange={(e) => setRulesDimensionFilter(e.target.value)}
              >
                <MenuItem value="">All</MenuItem>
                <MenuItem value="COMPLETENESS">Completeness</MenuItem>
                <MenuItem value="ACCURACY">Accuracy</MenuItem>
                <MenuItem value="CONSISTENCY">Consistency</MenuItem>
                <MenuItem value="VALIDITY">Validity</MenuItem>
                <MenuItem value="UNIQUENESS">Uniqueness</MenuItem>
                <MenuItem value="TIMELINESS">Timeliness</MenuItem>
              </Select>
            </FormControl>
            <FormControl sx={{ minWidth: 200 }}>
              <InputLabel>Filter by Severity</InputLabel>
              <Select
                value={rulesSeverityFilter}
                label="Filter by Severity"
                onChange={(e) => setRulesSeverityFilter(e.target.value)}
              >
                <MenuItem value="">All</MenuItem>
                <MenuItem value="LOW">Low</MenuItem>
                <MenuItem value="MEDIUM">Medium</MenuItem>
                <MenuItem value="HIGH">High</MenuItem>
                <MenuItem value="CRITICAL">Critical</MenuItem>
              </Select>
            </FormControl>
          </Box>

          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Dimension</TableCell>
                  <TableCell>Rule Type</TableCell>
                  <TableCell>Severity</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Created At</TableCell>
                  <TableCell align="center">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rules.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} align="center">
                      <Typography variant="body2" color="text.secondary">
                        No rules found
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  rules.map((rule) => (
                    <TableRow key={rule.id} hover>
                      <TableCell>{rule.name}</TableCell>
                      <TableCell>{rule.dimension}</TableCell>
                      <TableCell>{rule.ruleType}</TableCell>
                      <TableCell>
                        <Chip
                          label={rule.severity}
                          color={getSeverityColor(rule.severity) as any}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={rule.enabled ? 'Active' : 'Disabled'}
                          color={rule.enabled ? 'success' : 'default'}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>{new Date(rule.createdAt).toLocaleString()}</TableCell>
                      <TableCell align="center">
                        <Tooltip title="View Details">
                          <IconButton size="small">
                            <ViewIcon />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Edit">
                          <IconButton size="small">
                            <EditIcon />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Execute">
                          <IconButton
                            size="small"
                            color="primary"
                            onClick={() => handleExecuteRule(rule.id)}
                          >
                            <PlayIcon />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Delete">
                          <IconButton
                            size="small"
                            color="error"
                            onClick={() => handleDeleteRule(rule.id)}
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
              count={rulesTotalElements}
              page={rulesPage}
              onPageChange={(_, newPage) => setRulesPage(newPage)}
              rowsPerPage={rulesRowsPerPage}
              onRowsPerPageChange={(e) => {
                setRulesRowsPerPage(parseInt(e.target.value, 10));
                setRulesPage(0);
              }}
            />
          </TableContainer>
        </>
      )}

      {/* Issues Tab */}
      {activeTab === 1 && (
        <>
          <Box sx={{ mb: 2, display: 'flex', gap: 2 }}>
            <FormControl sx={{ minWidth: 200 }}>
              <InputLabel>Filter by Status</InputLabel>
              <Select
                value={issuesStatusFilter}
                label="Filter by Status"
                onChange={(e) => setIssuesStatusFilter(e.target.value)}
              >
                <MenuItem value="">All</MenuItem>
                <MenuItem value="OPEN">Open</MenuItem>
                <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
                <MenuItem value="RESOLVED">Resolved</MenuItem>
                <MenuItem value="IGNORED">Ignored</MenuItem>
              </Select>
            </FormControl>
            <FormControl sx={{ minWidth: 200 }}>
              <InputLabel>Filter by Severity</InputLabel>
              <Select
                value={issuesSeverityFilter}
                label="Filter by Severity"
                onChange={(e) => setIssuesSeverityFilter(e.target.value)}
              >
                <MenuItem value="">All</MenuItem>
                <MenuItem value="LOW">Low</MenuItem>
                <MenuItem value="MEDIUM">Medium</MenuItem>
                <MenuItem value="HIGH">High</MenuItem>
                <MenuItem value="CRITICAL">Critical</MenuItem>
              </Select>
            </FormControl>
          </Box>

          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Description</TableCell>
                  <TableCell>Rule</TableCell>
                  <TableCell>Dimension</TableCell>
                  <TableCell>Severity</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Detected At</TableCell>
                  <TableCell align="center">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {issues.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} align="center">
                      <Typography variant="body2" color="text.secondary">
                        No issues found
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  issues.map((issue) => (
                    <TableRow key={issue.id} hover>
                      <TableCell>{issue.description}</TableCell>
                      <TableCell>{issue.ruleName}</TableCell>
                      <TableCell>{issue.dimension}</TableCell>
                      <TableCell>
                        <Chip
                          label={issue.severity}
                          color={getSeverityColor(issue.severity) as any}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={issue.status}
                          color={getStatusColor(issue.status) as any}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>{new Date(issue.detectedAt).toLocaleString()}</TableCell>
                      <TableCell align="center">
                        <Tooltip title="View Details">
                          <IconButton size="small">
                            <ViewIcon />
                          </IconButton>
                        </Tooltip>
                        {issue.status !== 'RESOLVED' && (
                          <Tooltip title="Resolve">
                            <IconButton
                              size="small"
                              color="success"
                              onClick={() => handleResolveIssue(issue.id)}
                            >
                              <ResolveIcon />
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
              count={issuesTotalElements}
              page={issuesPage}
              onPageChange={(_, newPage) => setIssuesPage(newPage)}
              rowsPerPage={issuesRowsPerPage}
              onRowsPerPageChange={(e) => {
                setIssuesRowsPerPage(parseInt(e.target.value, 10));
                setIssuesPage(0);
              }}
            />
          </TableContainer>
        </>
      )}

      {/* Profiles Tab */}
      {activeTab === 2 && (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Dataset</TableCell>
                <TableCell>Overall Score</TableCell>
                <TableCell>Total Records</TableCell>
                <TableCell>Completeness</TableCell>
                <TableCell>Accuracy</TableCell>
                <TableCell>Profiled At</TableCell>
                <TableCell align="center">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {profiles.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center">
                    <Typography variant="body2" color="text.secondary">
                      No profiles found
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                profiles.map((profile) => (
                  <TableRow key={profile.id} hover>
                    <TableCell>{profile.datasetName}</TableCell>
                    <TableCell>
                      <Typography sx={{ color: getScoreColor(profile.overallScore || 0) }}>
                        {(profile.overallScore || 0).toFixed(1)}%
                      </Typography>
                    </TableCell>
                    <TableCell>{(profile.totalRecords || 0).toLocaleString()}</TableCell>
                    <TableCell>{(profile.completenessScore || 0).toFixed(1)}%</TableCell>
                    <TableCell>{(profile.accuracyScore || 0).toFixed(1)}%</TableCell>
                    <TableCell>{new Date(profile.profiledAt).toLocaleString()}</TableCell>
                    <TableCell align="center">
                      <Tooltip title="View Details">
                        <IconButton size="small">
                          <ViewIcon />
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
            count={profilesTotalElements}
            page={profilesPage}
            onPageChange={(_, newPage) => setProfilesPage(newPage)}
            rowsPerPage={profilesRowsPerPage}
            onRowsPerPageChange={(e) => {
              setProfilesRowsPerPage(parseInt(e.target.value, 10));
              setProfilesPage(0);
            }}
          />
        </TableContainer>
      )}

      {/* Create Rule Dialog */}
      <Dialog
        open={createRuleDialogOpen}
        onClose={() => setCreateRuleDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Create New Quality Rule</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField
              label="Name"
              fullWidth
              value={newRule.name}
              onChange={(e) => setNewRule({ ...newRule, name: e.target.value })}
            />
            <TextField
              label="Description"
              fullWidth
              multiline
              rows={3}
              value={newRule.description}
              onChange={(e) => setNewRule({ ...newRule, description: e.target.value })}
            />
            <FormControl fullWidth>
              <InputLabel>Rule Type</InputLabel>
              <Select
                value={newRule.ruleType}
                label="Rule Type"
                onChange={(e) => setNewRule({ ...newRule, ruleType: e.target.value as any })}
              >
                <MenuItem value="NULL_CHECK">Null Check</MenuItem>
                <MenuItem value="FORMAT_VALIDATION">Format Validation</MenuItem>
                <MenuItem value="RANGE_VALIDATION">Range Validation</MenuItem>
                <MenuItem value="UNIQUENESS">Uniqueness</MenuItem>
                <MenuItem value="REFERENTIAL_INTEGRITY">Referential Integrity</MenuItem>
                <MenuItem value="BUSINESS_RULE">Business Rule</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Dimension</InputLabel>
              <Select
                value={newRule.dimension}
                label="Dimension"
                onChange={(e) => setNewRule({ ...newRule, dimension: e.target.value as any })}
              >
                <MenuItem value="COMPLETENESS">Completeness</MenuItem>
                <MenuItem value="ACCURACY">Accuracy</MenuItem>
                <MenuItem value="CONSISTENCY">Consistency</MenuItem>
                <MenuItem value="VALIDITY">Validity</MenuItem>
                <MenuItem value="UNIQUENESS">Uniqueness</MenuItem>
                <MenuItem value="TIMELINESS">Timeliness</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Severity</InputLabel>
              <Select
                value={newRule.severity}
                label="Severity"
                onChange={(e) => setNewRule({ ...newRule, severity: e.target.value as any })}
              >
                <MenuItem value="LOW">Low</MenuItem>
                <MenuItem value="MEDIUM">Medium</MenuItem>
                <MenuItem value="HIGH">High</MenuItem>
                <MenuItem value="CRITICAL">Critical</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateRuleDialogOpen(false)}>Cancel</Button>
          <Button
            onClick={handleCreateRule}
            variant="contained"
            disabled={!newRule.name || !newRule.description}
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default DataQuality;
