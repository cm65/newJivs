import { createTheme, ThemeOptions } from '@mui/material/styles';
import { alpha } from '@mui/material/styles';

// Define custom theme extensions
declare module '@mui/material/styles' {
  interface Palette {
    border: {
      light: string;
      main: string;
      dark: string;
    };
    surface: {
      1: string;
      2: string;
      3: string;
    };
    status: {
      completed: string;
      inProgress: string;
      pending: string;
      failed: string;
      paused: string;
    };
  }

  interface PaletteOptions {
    border?: {
      light: string;
      main: string;
      dark: string;
    };
    surface?: {
      1: string;
      2: string;
      3: string;
    };
    status?: {
      completed: string;
      inProgress: string;
      pending: string;
      failed: string;
      paused: string;
    };
  }

  interface TypographyVariants {
    metric: React.CSSProperties;
    metricLabel: React.CSSProperties;
    dataLabel: React.CSSProperties;
  }

  interface TypographyVariantsOptions {
    metric?: React.CSSProperties;
    metricLabel?: React.CSSProperties;
    dataLabel?: React.CSSProperties;
  }
}

// Augment the theme to include custom typography variants
declare module '@mui/material/Typography' {
  interface TypographyPropsVariantOverrides {
    metric: true;
    metricLabel: true;
    dataLabel: true;
  }
}

// Create base theme configuration
const themeOptions: ThemeOptions = {
  palette: {
    mode: 'light',
    primary: {
      main: '#1976d2',
      light: '#42a5f5',
      dark: '#1565c0',
      contrastText: '#ffffff',
    },
    secondary: {
      main: '#dc004e',
      light: '#f44336',
      dark: '#c62828',
      contrastText: '#ffffff',
    },
    background: {
      default: '#f5f5f5',
      paper: '#ffffff',
    },
    text: {
      primary: 'rgba(0, 0, 0, 0.87)', // WCAG AA compliant
      secondary: 'rgba(0, 0, 0, 0.60)', // Increased from 0.54 for better contrast
      disabled: 'rgba(0, 0, 0, 0.38)',
    },
    success: {
      main: '#4caf50',
      light: '#81c784',
      dark: '#388e3c',
      contrastText: '#ffffff',
    },
    warning: {
      main: '#ff9800',
      light: '#ffb74d',
      dark: '#f57c00',
      contrastText: 'rgba(0, 0, 0, 0.87)',
    },
    error: {
      main: '#f44336',
      light: '#e57373',
      dark: '#d32f2f',
      contrastText: '#ffffff',
    },
    info: {
      main: '#2196f3',
      light: '#64b5f6',
      dark: '#1976d2',
      contrastText: '#ffffff',
    },
    divider: 'rgba(0, 0, 0, 0.12)',
    // Custom palette additions
    border: {
      light: '#e0e0e0',
      main: '#bdbdbd',
      dark: '#9e9e9e',
    },
    surface: {
      1: '#ffffff',
      2: '#f5f5f5',
      3: '#eeeeee',
    },
    status: {
      completed: '#4caf50',
      inProgress: '#2196f3',
      pending: '#ff9800',
      failed: '#f44336',
      paused: '#9e9e9e',
    },
  },
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    // Improved heading hierarchy with better weights
    h1: {
      fontSize: '2.5rem',
      fontWeight: 700,
      lineHeight: 1.2,
      letterSpacing: '-0.02em',
    },
    h2: {
      fontSize: '2rem',
      fontWeight: 600,
      lineHeight: 1.25,
      letterSpacing: '-0.01em',
    },
    h3: {
      fontSize: '1.75rem',
      fontWeight: 600,
      lineHeight: 1.3,
    },
    h4: {
      fontSize: '1.5rem',
      fontWeight: 600,
      lineHeight: 1.35,
    },
    h5: {
      fontSize: '1.25rem',
      fontWeight: 600,
      lineHeight: 1.4,
    },
    h6: {
      fontSize: '1rem',
      fontWeight: 600,
      lineHeight: 1.5,
    },
    // Improved body text for better readability
    body1: {
      fontSize: '0.875rem', // 14px for better density in enterprise apps
      lineHeight: 1.6,
    },
    body2: {
      fontSize: '0.8125rem', // 13px
      lineHeight: 1.5,
    },
    subtitle1: {
      fontSize: '1rem',
      fontWeight: 500,
      lineHeight: 1.5,
    },
    subtitle2: {
      fontSize: '0.875rem',
      fontWeight: 500,
      lineHeight: 1.45,
    },
    button: {
      fontSize: '0.875rem',
      fontWeight: 500,
      lineHeight: 1.75,
      letterSpacing: '0.02857em',
      textTransform: 'none' as const,
    },
    caption: {
      fontSize: '0.75rem',
      lineHeight: 1.66,
      letterSpacing: '0.03333em',
    },
    overline: {
      fontSize: '0.6875rem',
      fontWeight: 500,
      lineHeight: 2.66,
      letterSpacing: '0.08333em',
      textTransform: 'uppercase' as const,
    },
    // Custom typography variants
    metric: {
      fontSize: '2rem',
      fontWeight: 700,
      lineHeight: 1.2,
    },
    metricLabel: {
      fontSize: '0.75rem',
      fontWeight: 500,
      textTransform: 'uppercase',
      letterSpacing: '0.5px',
      lineHeight: 1.5,
    },
    dataLabel: {
      fontSize: '0.8125rem',
      fontWeight: 500,
      lineHeight: 1.5,
    },
  },
  shape: {
    borderRadius: 8,
  },
  spacing: 8,
  transitions: {
    easing: {
      easeInOut: 'cubic-bezier(0.4, 0, 0.2, 1)',
      easeOut: 'cubic-bezier(0.0, 0, 0.2, 1)',
      easeIn: 'cubic-bezier(0.4, 0, 1, 1)',
      sharp: 'cubic-bezier(0.4, 0, 0.6, 1)',
    },
    duration: {
      shortest: 150,
      shorter: 200,
      short: 250,
      standard: 300,
      complex: 375,
      enteringScreen: 225,
      leavingScreen: 195,
    },
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          borderRadius: 8,
          fontWeight: 500,
          transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
          '&:focus-visible': {
            outline: '2px solid',
            outlineColor: '#1976d2',
            outlineOffset: 2,
          },
        },
        contained: {
          boxShadow: 'none',
          '&:hover': {
            boxShadow: '0 2px 4px rgba(0,0,0,0.12)',
          },
        },
        sizeLarge: {
          minHeight: 48, // Ensure 48px touch target
          fontSize: '1rem',
        },
      },
    },
    MuiIconButton: {
      styleOverrides: {
        root: {
          '&:focus-visible': {
            outline: '2px solid',
            outlineColor: '#1976d2',
            outlineOffset: 2,
          },
        },
        sizeLarge: {
          padding: 12, // Ensure 48px touch target
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          boxShadow: '0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.24)',
          transition: 'box-shadow 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
          '&:hover': {
            boxShadow: '0 3px 6px rgba(0,0,0,0.15), 0 2px 4px rgba(0,0,0,0.12)',
          },
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: 'none',
        },
        elevation0: {
          border: '1px solid',
          borderColor: 'rgba(0, 0, 0, 0.12)',
        },
        elevation1: {
          boxShadow: '0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.24)',
        },
        elevation2: {
          boxShadow: '0 3px 6px rgba(0,0,0,0.15), 0 2px 4px rgba(0,0,0,0.12)',
        },
        elevation3: {
          boxShadow: '0 10px 20px rgba(0,0,0,0.15), 0 3px 6px rgba(0,0,0,0.10)',
        },
      },
    },
    MuiTextField: {
      styleOverrides: {
        root: {
          '& .MuiInputBase-root': {
            transition: 'border-color 0.3s ease',
          },
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          fontWeight: 500,
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          borderBottom: '1px solid rgba(0, 0, 0, 0.08)',
        },
        head: {
          fontWeight: 600,
          backgroundColor: '#fafafa',
        },
      },
    },
    MuiLink: {
      styleOverrides: {
        root: {
          '&:focus-visible': {
            outline: '2px solid',
            outlineColor: '#1976d2',
            outlineOffset: 2,
          },
        },
      },
    },
    MuiTooltip: {
      styleOverrides: {
        tooltip: {
          backgroundColor: 'rgba(0, 0, 0, 0.87)',
          fontSize: '0.75rem',
        },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          boxShadow: '0 1px 3px rgba(0,0,0,0.12)',
        },
      },
    },
    MuiDrawer: {
      styleOverrides: {
        paper: {
          borderRight: '1px solid rgba(0, 0, 0, 0.08)',
        },
      },
    },
    MuiListItemButton: {
      styleOverrides: {
        root: {
          '&:focus-visible': {
            outline: '2px solid',
            outlineColor: '#1976d2',
            outlineOffset: -2,
          },
        },
      },
    },
    MuiAlert: {
      styleOverrides: {
        root: {
          borderRadius: 8,
        },
      },
    },
    MuiSkeleton: {
      styleOverrides: {
        root: {
          backgroundColor: 'rgba(0, 0, 0, 0.08)',
        },
      },
    },
  },
};

// Create theme
export const theme = createTheme(themeOptions);

// Export commonly used colors for charts and other components
export const chartColors = {
  primary: ['#1976d2', '#42a5f5', '#90caf9', '#bbdefb'],
  success: ['#4caf50', '#66bb6a', '#81c784', '#a5d6a7'],
  warning: ['#ff9800', '#ffa726', '#ffb74d', '#ffcc80'],
  error: ['#f44336', '#ef5350', '#e57373', '#ef9a9a'],
  neutral: ['#9e9e9e', '#bdbdbd', '#e0e0e0', '#f5f5f5'],
};

// Export status colors for consistency
export const statusColors = {
  COMPLETED: theme.palette.success.main,
  RUNNING: theme.palette.info.main,
  IN_PROGRESS: theme.palette.info.main,
  PENDING: theme.palette.warning.main,
  FAILED: theme.palette.error.main,
  STOPPED: theme.palette.grey[600],
  PAUSED: theme.palette.grey[600],
  ROLLING_BACK: theme.palette.warning.dark,
};

// Export elevation styles for consistency
export const elevations = {
  card: '0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.24)',
  cardHover: '0 3px 6px rgba(0,0,0,0.15), 0 2px 4px rgba(0,0,0,0.12)',
  modal: '0 10px 20px rgba(0,0,0,0.15), 0 3px 6px rgba(0,0,0,0.10)',
  dropdown: '0 3px 6px rgba(0,0,0,0.15), 0 2px 4px rgba(0,0,0,0.12)',
};

// Export gradient backgrounds for premium feel
export const gradients = {
  primary: `linear-gradient(135deg, ${theme.palette.primary.main} 0%, ${theme.palette.primary.dark} 100%)`,
  success: `linear-gradient(135deg, ${theme.palette.success.main} 0%, ${theme.palette.success.dark} 100%)`,
  warning: `linear-gradient(135deg, ${theme.palette.warning.main} 0%, ${theme.palette.warning.dark} 100%)`,
  error: `linear-gradient(135deg, ${theme.palette.error.main} 0%, ${theme.palette.error.dark} 100%)`,
  info: `linear-gradient(135deg, ${theme.palette.info.main} 0%, ${theme.palette.info.dark} 100%)`,
  premium: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
};

// Export helper function for creating consistent focus styles
export const getFocusStyles = (color: string = theme.palette.primary.main) => ({
  '&:focus-visible': {
    outline: `2px solid ${color}`,
    outlineOffset: 2,
  },
});

// Export helper function for creating consistent hover states
export const getHoverStyles = (elevation: number = 2) => ({
  transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
  '&:hover': {
    transform: 'translateY(-2px)',
    boxShadow: theme.shadows[elevation + 1],
  },
});

// Export helper for creating accessible color contrast
export const ensureContrast = (bgColor: string): string => {
  // Simple helper - in production, use a proper contrast calculation
  const lightText = '#ffffff';
  const darkText = 'rgba(0, 0, 0, 0.87)';

  // This is a simplified check - implement proper luminance calculation
  const isLightBg = bgColor.startsWith('#f') || bgColor.startsWith('#e');
  return isLightBg ? darkText : lightText;
};

// Export screen reader only styles
export const visuallyHidden = {
  position: 'absolute' as const,
  width: 1,
  height: 1,
  padding: 0,
  margin: -1,
  overflow: 'hidden',
  clip: 'rect(0, 0, 0, 0)',
  whiteSpace: 'nowrap' as const,
  border: 0,
};

export default theme;