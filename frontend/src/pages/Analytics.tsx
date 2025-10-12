import React from 'react';
import { Box, Typography, Paper } from '@mui/material';

const Analytics: React.FC = () => {
  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" fontWeight="bold" gutterBottom>
        Analytics & Reports
      </Typography>
      <Typography variant="body2" color="text.secondary" gutterBottom>
        System analytics, reports, and compliance documentation
      </Typography>
      <Paper sx={{ p: 3, mt: 3 }}>
        <Typography variant="body1" color="text.secondary" align="center">
          Analytics and reporting page coming soon...
        </Typography>
      </Paper>
    </Box>
  );
};

export default Analytics;
