import React from 'react';
import {
  Grid,
  Paper,
  Skeleton,
  Box,
  Card,
  CardContent,
  useTheme,
} from '@mui/material';

const DashboardSkeleton: React.FC = () => {
  const theme = useTheme();

  return (
    <Box component="main" role="status" aria-label="Loading dashboard" sx={{ maxWidth: 1440, mx: 'auto' }}>
      {/* Header Skeleton */}
      <Box sx={{ mb: 4, pt: 2 }}>
        <Skeleton variant="text" width={200} height={48} sx={{ mb: 1 }} />
        <Skeleton variant="text" width={400} height={24} />
      </Box>

      {/* Stats Cards Skeleton */}
      <Box component="section" sx={{ mb: 4 }}>
        <Grid container spacing={3}>
          {[1, 2, 3, 4].map((item) => (
            <Grid item xs={12} sm={6} md={3} key={item}>
              <Card sx={{ height: '100%' }}>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <Skeleton
                      variant="rounded"
                      width={48}
                      height={48}
                      sx={{ mr: 2, borderRadius: 2 }}
                    />
                    <Box sx={{ flex: 1 }}>
                      <Skeleton variant="text" width="60%" height={40} />
                      <Skeleton variant="text" width="80%" height={20} />
                    </Box>
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center' }}>
                    <Skeleton variant="circular" width={20} height={20} sx={{ mr: 0.5 }} />
                    <Skeleton variant="text" width={120} height={20} />
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Box>

      {/* Section Header Skeleton */}
      <Skeleton variant="text" width={200} height={32} sx={{ mb: 2 }} />

      {/* Charts Section Skeleton */}
      <Grid container spacing={3}>
        {/* Line Chart Skeleton */}
        <Grid item xs={12} md={8}>
          <Paper
            elevation={0}
            sx={{
              p: 2,
              border: '1px solid',
              borderColor: theme.palette.divider,
            }}
          >
            <Skeleton variant="text" width={200} height={32} sx={{ mb: 2 }} />
            <Skeleton variant="rectangular" width="100%" height={300} sx={{ borderRadius: 1 }} />
          </Paper>
        </Grid>

        {/* Pie Chart Skeleton */}
        <Grid item xs={12} md={4}>
          <Paper
            elevation={0}
            sx={{
              p: 2,
              border: '1px solid',
              borderColor: theme.palette.divider,
            }}
          >
            <Skeleton variant="text" width={150} height={32} sx={{ mb: 2 }} />
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 300 }}>
              <Skeleton variant="circular" width={160} height={160} />
            </Box>
          </Paper>
        </Grid>

        {/* System Performance Skeleton */}
        <Grid item xs={12} md={6}>
          <Paper
            elevation={0}
            sx={{
              p: 2,
              border: '1px solid',
              borderColor: theme.palette.divider,
            }}
          >
            <Skeleton variant="text" width={180} height={32} sx={{ mb: 2 }} />
            <Box sx={{ mt: 2 }}>
              {[1, 2, 3, 4].map((item) => (
                <Box key={item} sx={{ mb: 2 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Skeleton variant="text" width={80} height={20} />
                    <Skeleton variant="text" width={40} height={20} />
                  </Box>
                  <Skeleton variant="rectangular" width="100%" height={8} sx={{ borderRadius: 4 }} />
                </Box>
              ))}
            </Box>
          </Paper>
        </Grid>

        {/* Recent Activities Skeleton */}
        <Grid item xs={12} md={6}>
          <Paper
            elevation={0}
            sx={{
              p: 2,
              border: '1px solid',
              borderColor: theme.palette.divider,
            }}
          >
            <Skeleton variant="text" width={160} height={32} sx={{ mb: 2 }} />
            <Box sx={{ mt: 2 }}>
              {[1, 2, 3, 4, 5].map((item) => (
                <Box
                  key={item}
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    py: 1,
                    borderBottom: item < 5 ? `1px solid ${theme.palette.divider}` : 'none',
                  }}
                >
                  <Box sx={{ display: 'flex', alignItems: 'center', flex: 1 }}>
                    <Skeleton variant="circular" width={20} height={20} sx={{ mr: 1 }} />
                    <Skeleton variant="text" width="60%" height={20} />
                  </Box>
                  <Skeleton variant="text" width={80} height={16} />
                </Box>
              ))}
            </Box>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default DashboardSkeleton;