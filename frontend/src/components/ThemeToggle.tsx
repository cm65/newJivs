import React from 'react';
import { IconButton, Tooltip, useTheme } from '@mui/material';
import { Brightness4 as DarkModeIcon, Brightness7 as LightModeIcon } from '@mui/icons-material';
import { useThemeMode } from '../contexts/ThemeContext';

interface ThemeToggleProps {
  color?: 'inherit' | 'default' | 'primary' | 'secondary';
  size?: 'small' | 'medium' | 'large';
}

const ThemeToggle: React.FC<ThemeToggleProps> = ({ color = 'inherit', size = 'medium' }) => {
  const theme = useTheme();
  const { mode, toggleTheme } = useThemeMode();

  const handleToggle = () => {
    toggleTheme();
  };

  return (
    <Tooltip title={mode === 'light' ? 'Switch to dark mode' : 'Switch to light mode'}>
      <IconButton
        onClick={handleToggle}
        color={color}
        size={size}
        aria-label={mode === 'light' ? 'Switch to dark mode' : 'Switch to light mode'}
        sx={{
          transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
          '&:hover': {
            transform: 'rotate(180deg)',
          },
          '&:focus-visible': {
            outline: '2px solid',
            outlineColor: theme.palette.primary.main,
            outlineOffset: 2,
          },
        }}
      >
        {mode === 'light' ? (
          <DarkModeIcon
            sx={{
              transition: 'transform 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
            }}
          />
        ) : (
          <LightModeIcon
            sx={{
              transition: 'transform 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
            }}
          />
        )}
      </IconButton>
    </Tooltip>
  );
};

export default ThemeToggle;
