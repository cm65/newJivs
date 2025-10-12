import React, { useState } from 'react';
import {
  SpeedDial,
  SpeedDialAction,
  SpeedDialIcon,
  Backdrop,
  useTheme,
  useMediaQuery,
} from '@mui/material';
import {
  Upload as UploadIcon,
  SyncAlt as SyncIcon,
  Assessment as AssessmentIcon,
  Gavel as GavelIcon,
  Close as CloseIcon,
  Add as AddIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';

interface QuickAction {
  icon: React.ReactElement;
  name: string;
  onClick: () => void;
  ariaLabel: string;
  color?: string;
}

interface QuickActionsProps {
  onCreateExtraction?: () => void;
  onStartMigration?: () => void;
  onRunQualityCheck?: () => void;
  onGenerateReport?: () => void;
}

const QuickActions: React.FC<QuickActionsProps> = ({
  onCreateExtraction,
  onStartMigration,
  onRunQualityCheck,
  onGenerateReport,
}) => {
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  const handleOpen = () => setOpen(true);
  const handleClose = () => setOpen(false);

  const defaultCreateExtraction = () => {
    navigate('/extractions', { state: { openDialog: true } });
    handleClose();
  };

  const defaultStartMigration = () => {
    navigate('/migrations', { state: { openDialog: true } });
    handleClose();
  };

  const defaultRunQualityCheck = () => {
    navigate('/data-quality', { state: { action: 'run-check' } });
    handleClose();
  };

  const defaultGenerateReport = () => {
    navigate('/analytics', { state: { action: 'generate-report' } });
    handleClose();
  };

  const actions: QuickAction[] = [
    {
      icon: <UploadIcon />,
      name: 'New Extraction',
      onClick: onCreateExtraction || defaultCreateExtraction,
      ariaLabel: 'Create new data extraction',
      color: theme.palette.primary.main,
    },
    {
      icon: <SyncIcon />,
      name: 'New Migration',
      onClick: onStartMigration || defaultStartMigration,
      ariaLabel: 'Start new data migration',
      color: theme.palette.secondary.main,
    },
    {
      icon: <AssessmentIcon />,
      name: 'Quality Check',
      onClick: onRunQualityCheck || defaultRunQualityCheck,
      ariaLabel: 'Run data quality check',
      color: theme.palette.warning.main,
    },
    {
      icon: <GavelIcon />,
      name: 'Compliance Report',
      onClick: onGenerateReport || defaultGenerateReport,
      ariaLabel: 'Generate compliance report',
      color: theme.palette.success.main,
    },
  ];

  return (
    <>
      <Backdrop
        open={open}
        sx={{
          zIndex: theme.zIndex.speedDial - 1,
          backgroundColor: 'rgba(0, 0, 0, 0.5)',
        }}
        onClick={handleClose}
      />
      <SpeedDial
        ariaLabel="Quick actions menu"
        sx={{
          position: 'fixed',
          bottom: isMobile ? 16 : 24,
          right: isMobile ? 16 : 24,
          '& .MuiSpeedDial-fab': {
            backgroundColor: theme.palette.primary.main,
            color: theme.palette.primary.contrastText,
            width: isMobile ? 48 : 56,
            height: isMobile ? 48 : 56,
            '&:hover': {
              backgroundColor: theme.palette.primary.dark,
            },
            boxShadow: theme.shadows[6],
            transition: theme.transitions.create(['transform', 'box-shadow'], {
              duration: theme.transitions.duration.short,
            }),
            '&:active': {
              transform: 'scale(0.95)',
            },
          },
        }}
        icon={<SpeedDialIcon icon={<AddIcon />} openIcon={<CloseIcon />} />}
        onClose={handleClose}
        onOpen={handleOpen}
        open={open}
        direction="up"
        FabProps={{
          'aria-label': 'Quick actions',
          'aria-expanded': open,
          'aria-haspopup': 'menu',
        }}
      >
        {actions.map((action) => (
          <SpeedDialAction
            key={action.name}
            icon={action.icon}
            tooltipTitle={action.name}
            tooltipOpen
            tooltipPlacement="left"
            onClick={action.onClick}
            FabProps={{
              'aria-label': action.ariaLabel,
              sx: {
                backgroundColor: 'background.paper',
                color: action.color || theme.palette.text.primary,
                '&:hover': {
                  backgroundColor: action.color
                    ? `${action.color}15`
                    : 'action.hover',
                  color: action.color,
                },
                boxShadow: theme.shadows[2],
                transition: theme.transitions.create(
                  ['background-color', 'color', 'transform'],
                  {
                    duration: theme.transitions.duration.short,
                  }
                ),
                '&:active': {
                  transform: 'scale(0.95)',
                },
              },
            }}
            sx={{
              '& .MuiSpeedDialAction-staticTooltipLabel': {
                whiteSpace: 'nowrap',
                backgroundColor: theme.palette.grey[900],
                color: theme.palette.common.white,
                padding: theme.spacing(0.5, 1),
                borderRadius: theme.shape.borderRadius,
                fontSize: '0.875rem',
                fontWeight: 500,
                boxShadow: theme.shadows[3],
              },
            }}
          />
        ))}
      </SpeedDial>
    </>
  );
};

export default QuickActions;
