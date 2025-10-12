import React from 'react';
import {
  Box,
  Button,
  Toolbar,
  Typography,
  Paper,
  Tooltip,
  IconButton,
} from '@mui/material';
import {
  Close as CloseIcon,
  PlayArrow as StartIcon,
  Stop as StopIcon,
  Pause as PauseIcon,
  Delete as DeleteIcon,
  FileDownload as ExportIcon,
  Replay as ResumeIcon,
} from '@mui/icons-material';

export interface BulkAction {
  id: string;
  label: string;
  icon: React.ReactNode;
  color?: 'primary' | 'secondary' | 'error' | 'warning' | 'info' | 'success';
  onClick: () => void;
  disabled?: boolean;
}

interface BulkActionsToolbarProps {
  selectedCount: number;
  actions: BulkAction[];
  onClearSelection: () => void;
}

/**
 * Toolbar component that appears when items are selected
 * Shows count of selected items and available bulk actions
 */
const BulkActionsToolbar: React.FC<BulkActionsToolbarProps> = ({
  selectedCount,
  actions,
  onClearSelection,
}) => {
  if (selectedCount === 0) {
    return null;
  }

  return (
    <Paper
      sx={{
        mb: 2,
        backgroundColor: 'primary.light',
        color: 'primary.contrastText',
      }}
      elevation={3}
    >
      <Toolbar>
        <Typography variant="subtitle1" component="div" sx={{ flex: '1 1 100%' }}>
          {selectedCount} {selectedCount === 1 ? 'item' : 'items'} selected
        </Typography>

        <Box sx={{ display: 'flex', gap: 1 }}>
          {actions.map((action) => (
            <Tooltip key={action.id} title={action.label}>
              <span>
                <Button
                  variant="contained"
                  color={action.color || 'primary'}
                  startIcon={action.icon}
                  onClick={action.onClick}
                  disabled={action.disabled}
                  sx={{
                    minWidth: 'auto',
                    px: 2,
                  }}
                >
                  {action.label}
                </Button>
              </span>
            </Tooltip>
          ))}

          <Tooltip title="Clear selection">
            <IconButton color="inherit" onClick={onClearSelection}>
              <CloseIcon />
            </IconButton>
          </Tooltip>
        </Box>
      </Toolbar>
    </Paper>
  );
};

export default BulkActionsToolbar;

/**
 * Pre-configured bulk action definitions for common operations
 */
export const BulkActionPresets = {
  start: (onClick: () => void, disabled = false): BulkAction => ({
    id: 'start',
    label: 'Start',
    icon: <StartIcon />,
    color: 'primary',
    onClick,
    disabled,
  }),

  stop: (onClick: () => void, disabled = false): BulkAction => ({
    id: 'stop',
    label: 'Stop',
    icon: <StopIcon />,
    color: 'warning',
    onClick,
    disabled,
  }),

  pause: (onClick: () => void, disabled = false): BulkAction => ({
    id: 'pause',
    label: 'Pause',
    icon: <PauseIcon />,
    color: 'warning',
    onClick,
    disabled,
  }),

  resume: (onClick: () => void, disabled = false): BulkAction => ({
    id: 'resume',
    label: 'Resume',
    icon: <ResumeIcon />,
    color: 'primary',
    onClick,
    disabled,
  }),

  delete: (onClick: () => void, disabled = false): BulkAction => ({
    id: 'delete',
    label: 'Delete',
    icon: <DeleteIcon />,
    color: 'error',
    onClick,
    disabled,
  }),

  export: (onClick: () => void, disabled = false): BulkAction => ({
    id: 'export',
    label: 'Export',
    icon: <ExportIcon />,
    color: 'info',
    onClick,
    disabled,
  }),
};
