import React from 'react';
import { Box, Typography, Paper } from '@mui/material';

const Documents: React.FC = () => {
  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" fontWeight="bold" gutterBottom>
        Documents
      </Typography>
      <Typography variant="body2" color="text.secondary" gutterBottom>
        Document archiving and storage management
      </Typography>
      <Paper sx={{ p: 3, mt: 3 }}>
        <Typography variant="body1" color="text.secondary" align="center">
          Documents management page coming soon...
        </Typography>
      </Paper>
    </Box>
  );
};

export default Documents;
