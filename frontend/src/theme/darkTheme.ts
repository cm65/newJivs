import { ThemeOptions } from '@mui/material/styles';
import { alpha } from '@mui/material/styles';

// Dark mode theme configuration with WCAG 2.1 AA compliant contrast ratios
export const darkThemeOptions: ThemeOptions = {
  palette: {
    mode: 'dark',
    primary: {
      main: '#90caf9', // Blue 300 - WCAG AA compliant on dark backgrounds
      light: '#bbdefb',
      dark: '#42a5f5',
      contrastText: 'rgba(0, 0, 0, 0.87)',
    },
    secondary: {
      main: '#ce93d8', // Purple 300 - WCAG AA compliant
      light: '#e1bee7',
      dark: '#ba68c8',
      contrastText: 'rgba(0, 0, 0, 0.87)',
    },
    background: {
      default: '#121212', // Material Design dark theme default
      paper: '#1e1e1e', // Elevated surface
    },
    text: {
      primary: 'rgba(255, 255, 255, 0.87)', // High emphasis - WCAG AA compliant
      secondary: 'rgba(255, 255, 255, 0.60)', // Medium emphasis
      disabled: 'rgba(255, 255, 255, 0.38)', // Disabled
    },
    success: {
      main: '#66bb6a', // Green 400 - adjusted for dark mode
      light: '#81c784',
      dark: '#4caf50',
      contrastText: 'rgba(0, 0, 0, 0.87)',
    },
    warning: {
      main: '#ffa726', // Orange 400 - adjusted for dark mode
      light: '#ffb74d',
      dark: '#ff9800',
      contrastText: 'rgba(0, 0, 0, 0.87)',
    },
    error: {
      main: '#ef5350', // Red 400 - adjusted for dark mode
      light: '#e57373',
      dark: '#f44336',
      contrastText: '#ffffff',
    },
    info: {
      main: '#64b5f6', // Light Blue 300 - adjusted for dark mode
      light: '#90caf9',
      dark: '#42a5f5',
      contrastText: 'rgba(0, 0, 0, 0.87)',
    },
    divider: 'rgba(255, 255, 255, 0.12)',
    // Custom palette additions
    border: {
      light: '#424242',
      main: '#616161',
      dark: '#757575',
    },
    surface: {
      1: '#1e1e1e', // Base elevated surface
      2: '#232323', // Higher elevation
      3: '#282828', // Highest elevation
    },
    status: {
      completed: '#66bb6a',
      inProgress: '#64b5f6',
      pending: '#ffa726',
      failed: '#ef5350',
      paused: '#9e9e9e',
    },
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          // Ensure smooth scrolling
          scrollbarColor: '#424242 #121212',
          '&::-webkit-scrollbar': {
            width: '12px',
          },
          '&::-webkit-scrollbar-track': {
            background: '#121212',
          },
          '&::-webkit-scrollbar-thumb': {
            backgroundColor: '#424242',
            borderRadius: '6px',
            border: '3px solid #121212',
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
          borderColor: 'rgba(255, 255, 255, 0.12)',
        },
        elevation1: {
          backgroundColor: '#1e1e1e',
          boxShadow: '0 1px 3px rgba(0,0,0,0.3), 0 1px 2px rgba(0,0,0,0.24)',
        },
        elevation2: {
          backgroundColor: '#232323',
          boxShadow: '0 3px 6px rgba(0,0,0,0.35), 0 2px 4px rgba(0,0,0,0.26)',
        },
        elevation3: {
          backgroundColor: '#282828',
          boxShadow: '0 10px 20px rgba(0,0,0,0.4), 0 3px 6px rgba(0,0,0,0.3)',
        },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          backgroundColor: '#1e1e1e',
          boxShadow: '0 1px 3px rgba(0,0,0,0.3)',
        },
      },
    },
    MuiDrawer: {
      styleOverrides: {
        paper: {
          backgroundColor: '#1e1e1e',
          borderRight: '1px solid rgba(255, 255, 255, 0.12)',
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          backgroundColor: '#1e1e1e',
          boxShadow: '0 1px 3px rgba(0,0,0,0.3), 0 1px 2px rgba(0,0,0,0.24)',
          '&:hover': {
            boxShadow: '0 3px 6px rgba(0,0,0,0.35), 0 2px 4px rgba(0,0,0,0.26)',
          },
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
        },
        head: {
          backgroundColor: '#1e1e1e',
          fontWeight: 600,
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          '&:focus-visible': {
            outline: '2px solid',
            outlineColor: '#90caf9',
            outlineOffset: 2,
          },
        },
      },
    },
    MuiIconButton: {
      styleOverrides: {
        root: {
          '&:focus-visible': {
            outline: '2px solid',
            outlineColor: '#90caf9',
            outlineOffset: 2,
          },
        },
      },
    },
    MuiTextField: {
      styleOverrides: {
        root: {
          '& .MuiOutlinedInput-root': {
            '& fieldset': {
              borderColor: 'rgba(255, 255, 255, 0.23)',
            },
            '&:hover fieldset': {
              borderColor: 'rgba(255, 255, 255, 0.4)',
            },
          },
        },
      },
    },
    MuiTooltip: {
      styleOverrides: {
        tooltip: {
          backgroundColor: 'rgba(97, 97, 97, 0.95)',
          fontSize: '0.75rem',
        },
      },
    },
    MuiSkeleton: {
      styleOverrides: {
        root: {
          backgroundColor: 'rgba(255, 255, 255, 0.11)',
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
    MuiAlert: {
      styleOverrides: {
        root: {
          borderRadius: 8,
        },
      },
    },
  },
};

// Export dark mode chart colors optimized for dark backgrounds
export const darkChartColors = {
  primary: ['#90caf9', '#64b5f6', '#42a5f5', '#2196f3'],
  success: ['#66bb6a', '#4caf50', '#43a047', '#388e3c'],
  warning: ['#ffa726', '#ff9800', '#fb8c00', '#f57c00'],
  error: ['#ef5350', '#f44336', '#e53935', '#d32f2f'],
  neutral: ['#bdbdbd', '#9e9e9e', '#757575', '#616161'],
};

// Export dark mode status colors
export const darkStatusColors = {
  COMPLETED: '#66bb6a',
  RUNNING: '#64b5f6',
  IN_PROGRESS: '#64b5f6',
  PENDING: '#ffa726',
  FAILED: '#ef5350',
  STOPPED: '#9e9e9e',
  PAUSED: '#9e9e9e',
  ROLLING_BACK: '#fb8c00',
};

// Export dark mode gradients
export const darkGradients = {
  primary: 'linear-gradient(135deg, #90caf9 0%, #42a5f5 100%)',
  success: 'linear-gradient(135deg, #66bb6a 0%, #43a047 100%)',
  warning: 'linear-gradient(135deg, #ffa726 0%, #fb8c00 100%)',
  error: 'linear-gradient(135deg, #ef5350 0%, #e53935 100%)',
  info: 'linear-gradient(135deg, #64b5f6 0%, #42a5f5 100%)',
  premium: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
};

export default darkThemeOptions;
