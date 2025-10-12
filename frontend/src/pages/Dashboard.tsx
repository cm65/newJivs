import React, { useEffect, useState } from 'react';
import {
  Grid,
  Paper,
  Typography,
  Box,
  LinearProgress,
  Alert,
  useTheme,
  Skeleton,
} from '@mui/material';
import {
  Storage as StorageIcon,
  Transform as TransformIcon,
  CheckCircle as CheckCircleIcon,
  Error as ErrorIcon,
  DataUsage as DataUsageIcon,
  Security as SecurityIcon,
} from '@mui/icons-material';
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import analyticsService, { DashboardAnalytics } from '../services/analyticsService';
import { statusColors, chartColors } from '../styles/theme';
import DashboardSkeleton from '../components/DashboardSkeleton';
import StatCard from '../components/StatCard';
import { useNavigate } from 'react-router-dom';

interface LoadingState {
  stats: boolean;
  charts: boolean;
  metrics: boolean;
  activities: boolean;
}

const Dashboard: React.FC = () => {
  const theme = useTheme();
  const navigate = useNavigate();
  const [analytics, setAnalytics] = useState<DashboardAnalytics | null>(null);
  const [loading, setLoading] = useState<LoadingState>({
    stats: true,
    charts: true,
    metrics: true,
    activities: true,
  });
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      // Phase 1: Load stats first (highest priority)
      setLoading(prev => ({ ...prev, stats: true }));
      const data = await analyticsService.getDashboardAnalytics();
      setAnalytics(data);
      setLoading(prev => ({ ...prev, stats: false }));
      setError(null);

      // Phase 2: Load charts (high priority) - delay to show progressive loading
      setTimeout(() => {
        setLoading(prev => ({ ...prev, charts: false }));
      }, 150);

      // Phase 3: Load metrics (medium priority)
      setTimeout(() => {
        setLoading(prev => ({ ...prev, metrics: false }));
      }, 300);

      // Phase 4: Load activities (lower priority)
      setTimeout(() => {
        setLoading(prev => ({ ...prev, activities: false }));
      }, 450);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load dashboard data');
      setLoading({
        stats: false,
        charts: false,
        metrics: false,
        activities: false,
      });
    }
  };

  // Show full skeleton only on initial load
  if (loading.stats && !analytics) {
    return <DashboardSkeleton />;
  }

  if (error && !analytics) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">{error}</Alert>
      </Box>
    );
  }

  // Sample data for charts - using theme colors
  const extractionData = [
    { month: 'Jan', jobs: 120, success: 115 },
    { month: 'Feb', jobs: 150, success: 142 },
    { month: 'Mar', jobs: 180, success: 175 },
    { month: 'Apr', jobs: 210, success: 202 },
    { month: 'May', jobs: 195, success: 190 },
    { month: 'Jun', jobs: 220, success: 215 },
  ];

  const migrationStatus = [
    { name: 'Completed', value: 45, color: statusColors.COMPLETED },
    { name: 'In Progress', value: 30, color: statusColors.IN_PROGRESS },
    { name: 'Pending', value: 15, color: statusColors.PENDING },
    { name: 'Failed', value: 10, color: statusColors.FAILED },
  ];

  const stats = analytics ? [
    {
      title: 'Total Extractions',
      value: analytics.totalExtractions,
      subtitle: 'All time',
      change: `${analytics.extractionSuccessRate.toFixed(1)}% success rate`,
      changeValue: 2.3,
      trend: 'up' as const,
      icon: <StorageIcon />,
      color: theme.palette.primary.main,
      bgColor: theme.palette.primary.light,
      format: 'number' as const,
      onClick: () => navigate('/extractions'),
    },
    {
      title: 'Active Migrations',
      value: analytics.totalMigrations,
      subtitle: 'Currently running',
      change: `${analytics.migrationSuccessRate.toFixed(1)}% success rate`,
      changeValue: 1.8,
      trend: 'up' as const,
      icon: <TransformIcon />,
      color: theme.palette.success.main,
      bgColor: theme.palette.success.light,
      format: 'number' as const,
      onClick: () => navigate('/migrations'),
    },
    {
      title: 'Data Quality Score',
      value: analytics.dataQualityScore.toFixed(1),
      subtitle: 'Last 30 days',
      changeValue: 2.3,
      trend: 'up' as const,
      icon: <CheckCircleIcon />,
      color: theme.palette.warning.main,
      bgColor: theme.palette.warning.light,
      format: 'percentage' as const,
      onClick: () => navigate('/data-quality'),
    },
    {
      title: 'Compliance Rate',
      value: analytics.complianceScore.toFixed(1),
      subtitle: 'GDPR & CCPA',
      changeValue: 1.1,
      trend: 'up' as const,
      icon: <SecurityIcon />,
      color: theme.palette.secondary.main,
      bgColor: theme.palette.secondary.light,
      format: 'percentage' as const,
      onClick: () => navigate('/compliance'),
    },
  ] : [];

  return (
    <Box component="main" role="main" aria-label="Dashboard" sx={{ maxWidth: 1440, mx: 'auto' }}>
      {/* Enhanced Header with better visual hierarchy */}
      <Box sx={{ mb: 4, pt: 2 }}>
        <Typography variant="h3" component="h1" fontWeight={600} gutterBottom>
          Dashboard
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ maxWidth: 600 }}>
          Real-time overview of your data integration, migration, and governance operations
        </Typography>
      </Box>

      {/* Stats Cards Section - Priority 1 */}
      <Box component="section" aria-labelledby="stats-heading">
        <Typography id="stats-heading" variant="h5" fontWeight={600} sx={{ mb: 2 }} className="visually-hidden">
          Key Statistics
        </Typography>
        <Grid container spacing={3} sx={{ mb: 4 }}>
          {loading.stats ? (
            // Show skeleton cards while stats are loading
            Array.from({ length: 4 }).map((_, index) => (
              <Grid item xs={12} sm={6} md={3} key={index}>
                <Paper
                  elevation={0}
                  sx={{
                    p: 2,
                    border: '1px solid',
                    borderColor: 'border.light',
                    height: '100%',
                  }}
                >
                  <Skeleton variant="circular" width={40} height={40} sx={{ mb: 2 }} />
                  <Skeleton variant="text" width="60%" height={24} sx={{ mb: 1 }} />
                  <Skeleton variant="text" width="40%" height={32} sx={{ mb: 1 }} />
                  <Skeleton variant="text" width="50%" height={20} />
                </Paper>
              </Grid>
            ))
          ) : (
            stats.map((stat, index) => (
              <Grid item xs={12} sm={6} md={3} key={index}>
                <StatCard {...stat} />
              </Grid>
            ))
          )}
        </Grid>
      </Box>

      {/* Charts Section - Priority 2 */}
      <Box component="section" aria-labelledby="charts-heading">
        <Typography id="charts-heading" variant="h5" fontWeight={600} sx={{ mb: 2 }}>
          Analytics & Insights
        </Typography>
        <Grid container spacing={3}>
          <Grid item xs={12} md={8}>
            <Paper
              elevation={0}
              sx={{
                p: 2,
                border: '1px solid',
                borderColor: 'border.light',
                minHeight: 370,
              }}
            >
              <Typography variant="h6" gutterBottom>
                Extraction Jobs Overview
              </Typography>
              {loading.charts ? (
                <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: 300 }}>
                  <Skeleton variant="rectangular" width="100%" height={200} sx={{ mb: 2 }} />
                  <Box sx={{ display: 'flex', gap: 3 }}>
                    <Skeleton variant="text" width={100} height={20} />
                    <Skeleton variant="text" width={100} height={20} />
                  </Box>
                </Box>
              ) : (
                <ResponsiveContainer
                  width="100%"
                  height={300}
                >
                  <LineChart data={extractionData}>
                    <CartesianGrid
                      strokeDasharray="3 3"
                      stroke={theme.palette.divider}
                      vertical={false}
                    />
                    <XAxis
                      dataKey="month"
                      axisLine={false}
                      tickLine={false}
                      tick={{ fill: theme.palette.text.secondary, fontSize: 12 }}
                    />
                    <YAxis
                      axisLine={false}
                      tickLine={false}
                      tick={{ fill: theme.palette.text.secondary, fontSize: 12 }}
                    />
                    <Tooltip
                      contentStyle={{
                        borderRadius: 8,
                        border: 'none',
                        boxShadow: '0 2px 8px rgba(0,0,0,0.15)'
                      }}
                    />
                    <Legend
                      iconType="circle"
                      wrapperStyle={{ paddingTop: 20 }}
                    />
                    <Line
                      type="monotone"
                      dataKey="jobs"
                      stroke={chartColors.primary[0]}
                      strokeWidth={2}
                      name="Total Jobs"
                      dot={{ fill: chartColors.primary[0], strokeWidth: 2 }}
                    />
                    <Line
                      type="monotone"
                      dataKey="success"
                      stroke={chartColors.success[0]}
                      strokeWidth={2}
                      name="Successful"
                      dot={{ fill: chartColors.success[0], strokeWidth: 2 }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              )}
            </Paper>
          </Grid>

          <Grid item xs={12} md={4}>
            <Paper
              elevation={0}
              sx={{
                p: 2,
                border: '1px solid',
                borderColor: 'border.light',
                minHeight: 370,
              }}
            >
              <Typography variant="h6" gutterBottom>
                Migration Status
              </Typography>
              {loading.charts ? (
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: 300 }}>
                  <Skeleton variant="circular" width={160} height={160} />
                </Box>
              ) : (
                <ResponsiveContainer
                  width="100%"
                  height={300}
                >
                  <PieChart>
                    <Pie
                      data={migrationStatus}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={(entry) => `${entry.name}: ${entry.value}`}
                      outerRadius={80}
                      fill={theme.palette.primary.main}
                      dataKey="value"
                    >
                      {migrationStatus.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </Paper>
          </Grid>

          {/* System Metrics Section - Priority 3 */}
          <Grid item xs={12} md={6}>
            <Paper
              elevation={0}
              sx={{
                p: 2,
                border: '1px solid',
                borderColor: 'border.light',
              }}
            >
              <Typography variant="h6" gutterBottom>
                System Performance
              </Typography>
              {loading.metrics ? (
                <Box sx={{ mt: 2 }}>
                  {Array.from({ length: 4 }).map((_, index) => (
                    <Box key={index} sx={{ mb: 2 }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                        <Skeleton variant="text" width={100} height={20} />
                        <Skeleton variant="text" width={40} height={20} />
                      </Box>
                      <Skeleton variant="rectangular" width="100%" height={8} sx={{ borderRadius: 4 }} />
                    </Box>
                  ))}
                </Box>
              ) : (
                <Box sx={{ mt: 2 }}>
                  <Box sx={{ mb: 2 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                      <Typography variant="body2">CPU Usage</Typography>
                      <Typography variant="body2" fontWeight={600}>45%</Typography>
                    </Box>
                    <LinearProgress
                      variant="determinate"
                      value={45}
                      sx={{ height: 8, borderRadius: 4 }}
                    />
                  </Box>
                  <Box sx={{ mb: 2 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                      <Typography variant="body2">Memory Usage</Typography>
                      <Typography variant="body2" fontWeight={600}>62%</Typography>
                    </Box>
                    <LinearProgress
                      variant="determinate"
                      value={62}
                      color="warning"
                      sx={{ height: 8, borderRadius: 4 }}
                    />
                  </Box>
                  <Box sx={{ mb: 2 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                      <Typography variant="body2">Storage</Typography>
                      <Typography variant="body2" fontWeight={600}>78%</Typography>
                    </Box>
                    <LinearProgress
                      variant="determinate"
                      value={78}
                      color="error"
                      sx={{ height: 8, borderRadius: 4 }}
                    />
                  </Box>
                  <Box>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                      <Typography variant="body2">Network</Typography>
                      <Typography variant="body2" fontWeight={600}>Normal</Typography>
                    </Box>
                    <LinearProgress
                      variant="determinate"
                      value={30}
                      color="success"
                      sx={{ height: 8, borderRadius: 4 }}
                    />
                  </Box>
                </Box>
              )}
            </Paper>
          </Grid>

          {/* Recent Activities Section - Priority 4 */}
          <Grid item xs={12} md={6}>
            <Paper
              elevation={0}
              sx={{
                p: 2,
                border: '1px solid',
                borderColor: 'border.light',
              }}
            >
              <Typography variant="h6" gutterBottom>
                Recent Activities
              </Typography>
              {loading.activities ? (
                <Box sx={{ mt: 2 }}>
                  {Array.from({ length: 5 }).map((_, index) => (
                    <Box
                      key={index}
                      sx={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        py: 1,
                        borderBottom: index < 4 ? `1px solid ${theme.palette.divider}` : 'none',
                      }}
                    >
                      <Box sx={{ display: 'flex', alignItems: 'center', flex: 1 }}>
                        <Skeleton variant="circular" width={20} height={20} sx={{ mr: 1 }} />
                        <Skeleton variant="text" width="70%" height={20} />
                      </Box>
                      <Skeleton variant="text" width={80} height={16} />
                    </Box>
                  ))}
                </Box>
              ) : (
                <Box sx={{ mt: 2 }}>
                  {[
                    { text: 'SAP extraction completed', time: '2 minutes ago', status: 'success' },
                    { text: 'Migration job #234 started', time: '15 minutes ago', status: 'info' },
                    { text: 'Data quality check failed', time: '1 hour ago', status: 'error' },
                    { text: 'Compliance report generated', time: '2 hours ago', status: 'success' },
                    { text: 'Document archived: INV-2024-001', time: '3 hours ago', status: 'info' },
                  ].map((activity, index) => (
                    <Box
                      key={index}
                      sx={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        py: 1,
                        borderBottom: index < 4 ? `1px solid ${theme.palette.divider}` : 'none',
                      }}
                    >
                      <Box sx={{ display: 'flex', alignItems: 'center' }}>
                        {activity.status === 'success' && (
                          <CheckCircleIcon fontSize="small" color="success" sx={{ mr: 1 }} />
                        )}
                        {activity.status === 'error' && (
                          <ErrorIcon fontSize="small" color="error" sx={{ mr: 1 }} />
                        )}
                        {activity.status === 'info' && (
                          <DataUsageIcon fontSize="small" color="primary" sx={{ mr: 1 }} />
                        )}
                        <Typography variant="body2">{activity.text}</Typography>
                      </Box>
                      <Typography variant="caption" color="text.secondary">
                        {activity.time}
                      </Typography>
                    </Box>
                  ))}
                </Box>
              )}
            </Paper>
          </Grid>
        </Grid>
      </Box>
    </Box>
  );
};

export default Dashboard;
