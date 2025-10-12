import React from 'react';
import {
  Box,
  Typography,
  Button,
  useTheme,
  Paper,
  SvgIcon,
} from '@mui/material';
import {
  Inbox as InboxIcon,
  Search as SearchIcon,
  CloudOff as CloudOffIcon,
  Error as ErrorIcon,
  FolderOpen as FolderIcon,
  Add as AddIcon,
} from '@mui/icons-material';

export interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description?: string;
  action?: {
    label: string;
    onClick: () => void;
    variant?: 'text' | 'outlined' | 'contained';
    startIcon?: React.ReactNode;
  };
  secondaryAction?: {
    label: string;
    onClick: () => void;
  };
  illustration?: 'default' | 'search' | 'error' | 'offline' | 'empty-folder' | 'no-data';
  size?: 'small' | 'medium' | 'large';
  elevation?: number;
  sx?: any;
}

const EmptyState: React.FC<EmptyStateProps> = ({
  icon,
  title,
  description,
  action,
  secondaryAction,
  illustration = 'default',
  size = 'medium',
  elevation = 0,
  sx = {},
}) => {
  const theme = useTheme();

  const getIllustration = () => {
    const iconSize = size === 'small' ? 64 : size === 'large' ? 120 : 80;
    const iconColor = theme.palette.action.disabled;

    switch (illustration) {
      case 'search':
        return <SearchIcon sx={{ fontSize: iconSize, color: iconColor }} />;
      case 'error':
        return <ErrorIcon sx={{ fontSize: iconSize, color: theme.palette.error.light }} />;
      case 'offline':
        return <CloudOffIcon sx={{ fontSize: iconSize, color: iconColor }} />;
      case 'empty-folder':
        return <FolderIcon sx={{ fontSize: iconSize, color: iconColor }} />;
      case 'no-data':
        return (
          <SvgIcon sx={{ fontSize: iconSize, color: iconColor }} viewBox="0 0 24 24">
            <path d="M9 2L7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 2H9zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z" />
          </SvgIcon>
        );
      default:
        return <InboxIcon sx={{ fontSize: iconSize, color: iconColor }} />;
    }
  };

  const getPadding = () => {
    switch (size) {
      case 'small':
        return 3;
      case 'large':
        return 6;
      default:
        return 4;
    }
  };

  const getTitleVariant = () => {
    switch (size) {
      case 'small':
        return 'h6';
      case 'large':
        return 'h4';
      default:
        return 'h5';
    }
  };

  const getDescriptionVariant = () => {
    switch (size) {
      case 'small':
        return 'body2';
      case 'large':
        return 'body1';
      default:
        return 'body1';
    }
  };

  return (
    <Paper
      elevation={elevation}
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        p: getPadding(),
        minHeight: size === 'small' ? 200 : size === 'large' ? 400 : 300,
        backgroundColor: elevation === 0 ? 'transparent' : undefined,
        ...sx,
      }}
    >
      <Box
        sx={{
          mb: 2,
          opacity: 0.8,
          animation: 'fadeIn 0.5s ease-in',
          '@keyframes fadeIn': {
            from: { opacity: 0, transform: 'translateY(-10px)' },
            to: { opacity: 0.8, transform: 'translateY(0)' },
          },
        }}
      >
        {icon || getIllustration()}
      </Box>

      <Typography
        variant={getTitleVariant()}
        component="h3"
        gutterBottom
        sx={{
          fontWeight: 600,
          color: theme.palette.text.primary,
          mb: description ? 1 : 3,
        }}
      >
        {title}
      </Typography>

      {description && (
        <Typography
          variant={getDescriptionVariant()}
          color="text.secondary"
          sx={{
            mb: 3,
            maxWidth: 400,
            lineHeight: 1.6,
          }}
        >
          {description}
        </Typography>
      )}

      {(action || secondaryAction) && (
        <Box
          sx={{
            display: 'flex',
            gap: 2,
            flexDirection: { xs: 'column', sm: 'row' },
            alignItems: 'center',
          }}
        >
          {action && (
            <Button
              variant={action.variant || 'contained'}
              onClick={action.onClick}
              startIcon={action.startIcon}
              size={size === 'small' ? 'small' : size === 'large' ? 'large' : 'medium'}
              sx={{
                minWidth: 120,
                '&:focus-visible': {
                  outline: '2px solid',
                  outlineColor: theme.palette.primary.main,
                  outlineOffset: 2,
                },
              }}
            >
              {action.label}
            </Button>
          )}

          {secondaryAction && (
            <Button
              variant="text"
              onClick={secondaryAction.onClick}
              size={size === 'small' ? 'small' : size === 'large' ? 'large' : 'medium'}
              sx={{
                minWidth: 100,
                '&:focus-visible': {
                  outline: '2px solid',
                  outlineColor: theme.palette.primary.main,
                  outlineOffset: 2,
                },
              }}
            >
              {secondaryAction.label}
            </Button>
          )}
        </Box>
      )}
    </Paper>
  );
};

// Pre-configured empty states for common scenarios

export const NoDataEmptyState: React.FC<Omit<EmptyStateProps, 'title' | 'illustration'>> = (props) => (
  <EmptyState
    {...props}
    title="No data available"
    illustration="no-data"
    description={props.description || "We couldn't find any data to display. Try adjusting your filters or create your first item."}
  />
);

export const NoSearchResultsEmptyState: React.FC<Omit<EmptyStateProps, 'title' | 'illustration'>> = (props) => (
  <EmptyState
    {...props}
    title="No results found"
    illustration="search"
    description={props.description || "Try adjusting your search terms or filters to find what you're looking for."}
  />
);

export const ErrorEmptyState: React.FC<Omit<EmptyStateProps, 'title' | 'illustration'>> = (props) => (
  <EmptyState
    {...props}
    title="Something went wrong"
    illustration="error"
    description={props.description || "We encountered an error while loading this content. Please try again."}
  />
);

export const OfflineEmptyState: React.FC<Omit<EmptyStateProps, 'title' | 'illustration'>> = (props) => (
  <EmptyState
    {...props}
    title="You're offline"
    illustration="offline"
    description={props.description || "Check your internet connection and try again."}
  />
);

export const EmptyFolderState: React.FC<Omit<EmptyStateProps, 'title' | 'illustration'>> = (props) => (
  <EmptyState
    {...props}
    title="This folder is empty"
    illustration="empty-folder"
    description={props.description || "Upload files or create new documents to get started."}
  />
);

// Specific empty states for JiVS platform features

export const NoExtractionsEmptyState: React.FC<{
  onCreateExtraction?: () => void;
  onImport?: () => void;
}> = ({ onCreateExtraction, onImport }) => (
  <EmptyState
    title="No extractions yet"
    description="Start extracting data from various sources like databases, APIs, and files."
    illustration="no-data"
    action={
      onCreateExtraction
        ? {
            label: 'Create Extraction',
            onClick: onCreateExtraction,
            variant: 'contained',
            startIcon: <AddIcon />,
          }
        : undefined
    }
    secondaryAction={
      onImport
        ? {
            label: 'Import Configuration',
            onClick: onImport,
          }
        : undefined
    }
  />
);

export const NoMigrationsEmptyState: React.FC<{
  onCreateMigration?: () => void;
  onViewDocs?: () => void;
}> = ({ onCreateMigration, onViewDocs }) => (
  <EmptyState
    title="No migrations found"
    description="Create your first data migration to move data between systems seamlessly."
    illustration="no-data"
    action={
      onCreateMigration
        ? {
            label: 'Start Migration',
            onClick: onCreateMigration,
            variant: 'contained',
            startIcon: <AddIcon />,
          }
        : undefined
    }
    secondaryAction={
      onViewDocs
        ? {
            label: 'View Documentation',
            onClick: onViewDocs,
          }
        : undefined
    }
  />
);

export const NoQualityIssuesEmptyState: React.FC = () => (
  <EmptyState
    title="No quality issues detected"
    description="Your data meets all quality standards. Great job!"
    illustration="default"
    size="small"
  />
);

export const NoComplianceRequestsEmptyState: React.FC = () => (
  <EmptyState
    title="No compliance requests"
    description="There are no pending GDPR or CCPA requests at this time."
    illustration="default"
    size="small"
  />
);

export const NoNotificationsEmptyState: React.FC = () => (
  <EmptyState
    title="All caught up!"
    description="You have no new notifications."
    illustration="default"
    size="small"
  />
);

export default EmptyState;