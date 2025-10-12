import React, { useState } from 'react';
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material';
import {
  Delete as DeleteIcon,
  PlayArrow as StartIcon,
  Stop as StopIcon,
  Pause as PauseIcon,
  Replay as ResumeIcon,
  Close as CloseIcon,
  FileDownload as ExportIcon,
} from '@mui/icons-material';

export interface BulkAction {
  id: string;
  label: string;
  icon: React.ReactNode;
  color?: 'primary' | 'secondary' | 'error' | 'warning' | 'info' | 'success';
  confirmMessage?: string;
  onExecute: (selectedIds: string[]) => Promise<void>;
}

interface BulkOperationsToolbarProps {
  selectedIds: string[];
  onClearSelection: () => void;
  actions: BulkAction[];
  entityName?: string; // e.g., "extractions", "migrations"
}

/**
 * Reusable bulk operations toolbar component
 * Part of Sprint 2 - Workflow 4: Bulk Operations
 */
const BulkOperationsToolbar: React.FC<BulkOperationsToolbarProps> = ({
  selectedIds,
  onClearSelection,
  actions,
  entityName = 'items',
}) => {
  const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);
  const [currentAction, setCurrentAction] = useState<BulkAction | null>(null);
  const [executing, setExecuting] = useState(false);

  const handleActionClick = (action: BulkAction) => {
    if (action.confirmMessage) {
      setCurrentAction(action);
      setConfirmDialogOpen(true);
    } else {
      executeAction(action);
    }
  };

  const executeAction = async (action: BulkAction) => {
    try {
      setExecuting(true);
      await action.onExecute(selectedIds);
      setConfirmDialogOpen(false);
      setCurrentAction(null);
      onClearSelection();
    } catch (error) {
      console.error(`Failed to execute bulk action ${action.id}:`, error);
    } finally {
      setExecuting(false);
    }
  };

  const handleConfirm = () => {
    if (currentAction) {
      executeAction(currentAction);
    }
  };

  const handleCancel = () => {
    setConfirmDialogOpen(false);
    setCurrentAction(null);
  };

  if (selectedIds.length === 0) {
    return null;
  }

  return (
    <>
      <Toolbar
        sx={{
          pl: { sm: 2 },
          pr: { xs: 1, sm: 1 },
          bgcolor: 'primary.main',
          color: 'primary.contrastText',
          borderRadius: 1,
          mb: 2,
        }}
      >
        <Box sx={{ flex: '1 1 100%', display: 'flex', alignItems: 'center', gap: 2 }}>
          <Chip
            label={`${selectedIds.length} selected`}
            color="default"
            sx={{ bgcolor: 'rgba(255,255,255,0.2)', color: 'white' }}
          />
          <Typography variant="body2" component="div">
            Bulk operations on {selectedIds.length} {entityName}
          </Typography>
        </Box>

        <Box sx={{ display: 'flex', gap: 1 }}>
          {actions.map((action) => (
            <Tooltip key={action.id} title={action.label}>
              <IconButton
                size="small"
                onClick={() => handleActionClick(action)}
                disabled={executing}
                sx={{
                  color: 'white',
                  '&:hover': {
                    bgcolor: 'rgba(255,255,255,0.1)',
                  },
                }}
              >
                {action.icon}
              </IconButton>
            </Tooltip>
          ))}

          <Tooltip title="Clear Selection">
            <IconButton
              size="small"
              onClick={onClearSelection}
              sx={{
                color: 'white',
                '&:hover': {
                  bgcolor: 'rgba(255,255,255,0.1)',
                },
              }}
            >
              <CloseIcon />
            </IconButton>
          </Tooltip>
        </Box>
      </Toolbar>

      {/* Confirmation Dialog */}
      <Dialog open={confirmDialogOpen} onClose={handleCancel}>
        <DialogTitle>Confirm Bulk Action</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {currentAction?.confirmMessage || 'Are you sure you want to perform this action?'}
          </DialogContentText>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
            This will affect <strong>{selectedIds.length}</strong> {entityName}.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCancel} disabled={executing}>
            Cancel
          </Button>
          <Button
            onClick={handleConfirm}
            variant="contained"
            color={currentAction?.color || 'primary'}
            disabled={executing}
            autoFocus
          >
            {executing ? 'Processing...' : 'Confirm'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default BulkOperationsToolbar;

// Predefined bulk actions for common use cases
export const createStartAction = (onExecute: (ids: string[]) => Promise<void>): BulkAction => ({
  id: 'start',
  label: 'Start Selected',
  icon: <StartIcon />,
  color: 'primary',
  onExecute,
});

export const createStopAction = (onExecute: (ids: string[]) => Promise<void>): BulkAction => ({
  id: 'stop',
  label: 'Stop Selected',
  icon: <StopIcon />,
  color: 'error',
  confirmMessage: 'Are you sure you want to stop the selected items?',
  onExecute,
});

export const createPauseAction = (onExecute: (ids: string[]) => Promise<void>): BulkAction => ({
  id: 'pause',
  label: 'Pause Selected',
  icon: <PauseIcon />,
  color: 'warning',
  onExecute,
});

export const createResumeAction = (onExecute: (ids: string[]) => Promise<void>): BulkAction => ({
  id: 'resume',
  label: 'Resume Selected',
  icon: <ResumeIcon />,
  color: 'primary',
  onExecute,
});

export const createDeleteAction = (onExecute: (ids: string[]) => Promise<void>): BulkAction => ({
  id: 'delete',
  label: 'Delete Selected',
  icon: <DeleteIcon />,
  color: 'error',
  confirmMessage: 'Are you sure you want to delete the selected items? This action cannot be undone.',
  onExecute,
});

export const createExportAction = (onExecute: (ids: string[]) => Promise<void>): BulkAction => ({
  id: 'export',
  label: 'Export Selected',
  icon: <ExportIcon />,
  color: 'info',
  onExecute,
});
