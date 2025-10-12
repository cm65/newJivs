import React, { createContext, useContext, useState, useEffect, useMemo, ReactNode } from 'react';
import { ThemeProvider as MuiThemeProvider, createTheme } from '@mui/material/styles';
import { themeOptions } from '../styles/theme';
import { darkThemeOptions } from '../theme/darkTheme';
import { useAuth } from './AuthContext';
import { preferencesService } from '../services/preferencesService';

export type ThemeMode = 'light' | 'dark';

interface ThemeContextType {
  mode: ThemeMode;
  toggleTheme: () => void;
  setThemeMode: (mode: ThemeMode) => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export const useThemeMode = (): ThemeContextType => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useThemeMode must be used within a ThemeModeProvider');
  }
  return context;
};

interface ThemeModeProviderProps {
  children: ReactNode;
}

export const ThemeModeProvider: React.FC<ThemeModeProviderProps> = ({ children }) => {
  const { user } = useAuth();
  const [mode, setMode] = useState<ThemeMode>(() => {
    // 1. Check localStorage first
    const savedMode = localStorage.getItem('themeMode') as ThemeMode | null;
    if (savedMode === 'light' || savedMode === 'dark') {
      return savedMode;
    }

    // 2. Fall back to system preference
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      return 'dark';
    }

    // 3. Default to light
    return 'light';
  });

  // Listen to system preference changes
  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

    const handleChange = (e: MediaQueryListEvent) => {
      // Only auto-switch if user hasn't set a preference
      const savedMode = localStorage.getItem('themeMode');
      if (!savedMode) {
        setMode(e.matches ? 'dark' : 'light');
      }
    };

    // Modern browsers
    if (mediaQuery.addEventListener) {
      mediaQuery.addEventListener('change', handleChange);
      return () => mediaQuery.removeEventListener('change', handleChange);
    } else {
      // Legacy browsers
      mediaQuery.addListener(handleChange);
      return () => mediaQuery.removeListener(handleChange);
    }
  }, []);

  // Load user preference from backend when user logs in
  useEffect(() => {
    const loadUserPreference = async () => {
      if (user) {
        try {
          const preferences = await preferencesService.getThemePreference();
          if (preferences.theme === 'light' || preferences.theme === 'dark') {
            setMode(preferences.theme);
            localStorage.setItem('themeMode', preferences.theme);
          }
        } catch (error) {
          console.error('Failed to load theme preference:', error);
          // Continue with current mode if backend fails
        }
      }
    };

    loadUserPreference();
  }, [user]);

  // Save preference to localStorage and backend
  const savePreference = async (newMode: ThemeMode) => {
    // Save to localStorage immediately
    localStorage.setItem('themeMode', newMode);

    // Save to backend if user is logged in
    if (user) {
      try {
        await preferencesService.updateThemePreference(newMode);
      } catch (error) {
        console.error('Failed to save theme preference:', error);
        // Continue even if backend save fails
      }
    }
  };

  const toggleTheme = () => {
    const newMode = mode === 'light' ? 'dark' : 'light';
    setMode(newMode);
    savePreference(newMode);
  };

  const setThemeMode = (newMode: ThemeMode) => {
    setMode(newMode);
    savePreference(newMode);
  };

  // Create theme based on mode
  const theme = useMemo(() => {
    const baseOptions = mode === 'light' ? themeOptions : darkThemeOptions;
    return createTheme(baseOptions);
  }, [mode]);

  const value = {
    mode,
    toggleTheme,
    setThemeMode,
  };

  return (
    <ThemeContext.Provider value={value}>
      <MuiThemeProvider theme={theme}>
        {children}
      </MuiThemeProvider>
    </ThemeContext.Provider>
  );
};

export default ThemeModeProvider;
