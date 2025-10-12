import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  TextField,
  Tooltip,
  FormControlLabel,
  Checkbox,
  Alert,
} from '@mui/material';
import {
  Save as SaveIcon,
  Delete as DeleteIcon,
  Share as ShareIcon,
} from '@mui/icons-material';
import { FilterGroup } from './FilterBuilder';
import viewsService, { SavedView } from '../services/viewsService';

interface SavedViewsProps {
  module: string;
  currentFilters: FilterGroup[];
  currentSort?: { field: string; direction: 'asc' | 'desc' }[];
  onApply: (view: SavedView) => void;
}

const SavedViews: React.FC<SavedViewsProps> = ({
  module,
  currentFilters,
  currentSort,
  onApply,
}) => {
  const [views, setViews] = useState<SavedView[]>([]);
  const [selectedView, setSelectedView] = useState<string>('');
  const [saveDialogOpen, setSaveDialogOpen] = useState(false);
  const [newViewName, setNewViewName] = useState('');
  const [isShared, setIsShared] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadViews();
  }, [module]);

  const loadViews = async () => {
    try {
      const data = await viewsService.getViews(module);
      setViews(data);
      setError(null);
    } catch (err: any) {
      setError('Failed to load saved views');
    }
  };

  const handleSaveView = async () => {
    if (!newViewName.trim()) {
      setError('View name is required');
      return;
    }

    try {
      await viewsService.saveView({
        name: newViewName,
        module,
        filters: currentFilters,
        sort: currentSort || [],
        isShared,
      });

      setNewViewName('');
      setIsShared(false);
      setSaveDialogOpen(false);
      loadViews();
      setError(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save view');
    }
  };

  const handleDeleteView = async (id: string) => {
    if (window.confirm('Are you sure you want to delete this saved view?')) {
      try {
        await viewsService.deleteView(id);
        if (selectedView === id) {
          setSelectedView('');
        }
        loadViews();
        setError(null);
      } catch (err: any) {
        setError('Failed to delete view');
      }
    }
  };

  const handleApplyView = async (viewId: string) => {
    try {
      const view = views.find((v) => v.id === viewId);
      if (view) {
        setSelectedView(viewId);
        onApply(view);
      }
    } catch (err: any) {
      setError('Failed to apply view');
    }
  };

  return (
    <>
      <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
        <FormControl sx={{ minWidth: 200 }}>
          <InputLabel size="small">Saved Views</InputLabel>
          <Select
            size="small"
            value={selectedView}
            label="Saved Views"
            onChange={(e) => handleApplyView(e.target.value)}
          >
            <MenuItem value="">
              <em>None</em>
            </MenuItem>
            {views.map((view) => (
              <MenuItem key={view.id} value={view.id}>
                {view.name}
                {view.isShared && ' (Shared)'}
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        <Tooltip title="Save Current View">
          <IconButton
            size="small"
            color="primary"
            onClick={() => setSaveDialogOpen(true)}
            disabled={currentFilters.length === 0 && (!currentSort || currentSort.length === 0)}
          >
            <SaveIcon />
          </IconButton>
        </Tooltip>

        {selectedView && (
          <Tooltip title="Delete View">
            <IconButton
              size="small"
              color="error"
              onClick={() => handleDeleteView(selectedView)}
            >
              <DeleteIcon />
            </IconButton>
          </Tooltip>
        )}
      </Box>

      {error && (
        <Alert severity="error" sx={{ mt: 1 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Save View Dialog */}
      <Dialog open={saveDialogOpen} onClose={() => setSaveDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Save Current View</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField
              label="View Name"
              fullWidth
              value={newViewName}
              onChange={(e) => setNewViewName(e.target.value)}
              placeholder="e.g., Active Extractions"
              autoFocus
            />
            <FormControlLabel
              control={
                <Checkbox checked={isShared} onChange={(e) => setIsShared(e.target.checked)} />
              }
              label="Share with team"
            />
            <Box sx={{ p: 2, backgroundColor: 'grey.100', borderRadius: 1 }}>
              <Box sx={{ mb: 1 }}>
                <strong>Filters:</strong> {currentFilters.length} group(s)
              </Box>
              <Box>
                <strong>Sort:</strong> {currentSort?.length || 0} column(s)
              </Box>
            </Box>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSaveDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleSaveView} variant="contained" disabled={!newViewName.trim()}>
            Save View
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default SavedViews;
