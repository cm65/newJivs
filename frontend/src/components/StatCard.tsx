import React from 'react';
import {
  Card,
  CardContent,
  Box,
  Typography,
  useTheme,
  Chip,
  CardActionArea,
} from '@mui/material';
import {
  TrendingUp as TrendingUpIcon,
  TrendingDown as TrendingDownIcon,
  TrendingFlat as TrendingFlatIcon,
} from '@mui/icons-material';
import { getHoverStyles } from '../styles/theme';

export interface StatCardProps {
  title: string;
  value: string | number;
  change?: string;
  changeValue?: number;
  trend?: 'up' | 'down' | 'neutral';
  icon: React.ReactNode;
  color?: string;
  bgColor?: string;
  onClick?: () => void;
  ariaLabel?: string;
  subtitle?: string;
  format?: 'number' | 'percentage' | 'currency' | 'custom';
  loading?: boolean;
}

const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  change,
  changeValue,
  trend = 'neutral',
  icon,
  color,
  bgColor,
  onClick,
  ariaLabel,
  subtitle,
  format = 'custom',
  loading = false,
}) => {
  const theme = useTheme();

  // Determine colors based on props or defaults
  const cardColor = color || theme.palette.primary.main;
  const cardBgColor = bgColor || theme.palette.primary.light;

  // Get trend icon and color
  const getTrendIcon = () => {
    switch (trend) {
      case 'up':
        return <TrendingUpIcon fontSize="small" />;
      case 'down':
        return <TrendingDownIcon fontSize="small" />;
      default:
        return <TrendingFlatIcon fontSize="small" />;
    }
  };

  const getTrendColor = () => {
    switch (trend) {
      case 'up':
        return theme.palette.success.main;
      case 'down':
        return theme.palette.error.main;
      default:
        return theme.palette.text.secondary;
    }
  };

  // Format value based on type
  const formatValue = (val: string | number): string => {
    if (typeof val === 'string') return val;

    switch (format) {
      case 'number':
        return val.toLocaleString();
      case 'percentage':
        return `${val}%`;
      case 'currency':
        return `$${val.toLocaleString()}`;
      default:
        return val.toString();
    }
  };

  const cardContent = (
    <CardContent>
      <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
        <Box sx={{ flex: 1 }}>
          {/* Icon and Title Row */}
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: 48,
                height: 48,
                borderRadius: 2,
                backgroundColor: `${cardBgColor}20`,
                color: cardColor,
                mr: 2,
                transition: 'transform 0.3s ease',
                ...(onClick && {
                  '&:hover': {
                    transform: 'scale(1.1)',
                  },
                }),
              }}
              aria-hidden="true"
            >
              {icon}
            </Box>
            <Box sx={{ flex: 1 }}>
              <Typography
                variant="metricLabel"
                color="text.secondary"
                sx={{ textTransform: 'uppercase', letterSpacing: '0.5px' }}
              >
                {title}
              </Typography>
              {subtitle && (
                <Typography variant="caption" color="text.disabled">
                  {subtitle}
                </Typography>
              )}
            </Box>
          </Box>

          {/* Value */}
          <Typography
            variant="metric"
            component="div"
            sx={{
              fontSize: '2rem',
              fontWeight: 700,
              lineHeight: 1.2,
              color: theme.palette.text.primary,
            }}
          >
            {loading ? '---' : formatValue(value)}
          </Typography>

          {/* Change Indicator */}
          {(change || changeValue !== undefined) && !loading && (
            <Box sx={{ display: 'flex', alignItems: 'center', mt: 1 }}>
              <Box
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  color: getTrendColor(),
                  mr: 1,
                }}
              >
                {getTrendIcon()}
              </Box>
              <Typography
                variant="body2"
                sx={{
                  color: getTrendColor(),
                  fontWeight: 500,
                }}
              >
                {changeValue !== undefined ? (
                  <>
                    {trend === 'up' ? '+' : trend === 'down' ? '-' : ''}
                    {Math.abs(changeValue).toFixed(1)}%
                  </>
                ) : (
                  change
                )}
              </Typography>
              {change && changeValue !== undefined && (
                <Typography
                  variant="caption"
                  color="text.secondary"
                  sx={{ ml: 1 }}
                >
                  {change}
                </Typography>
              )}
            </Box>
          )}
        </Box>

        {/* Optional status indicator */}
        {onClick && (
          <Chip
            label="View"
            size="small"
            variant="outlined"
            sx={{
              borderColor: cardColor,
              color: cardColor,
              '&:hover': {
                backgroundColor: `${cardColor}10`,
              },
            }}
          />
        )}
      </Box>
    </CardContent>
  );

  if (onClick) {
    return (
      <Card
        sx={{
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          ...getHoverStyles(1),
          cursor: 'pointer',
        }}
        role="button"
        tabIndex={0}
        onClick={onClick}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            onClick();
          }
        }}
        aria-label={ariaLabel || `${title}: ${value}. Click to view details`}
      >
        <CardActionArea sx={{ height: '100%' }}>
          {cardContent}
        </CardActionArea>
      </Card>
    );
  }

  return (
    <Card
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        ...getHoverStyles(1),
      }}
      aria-label={ariaLabel || `${title}: ${value}`}
    >
      {cardContent}
    </Card>
  );
};

// Create variant components for common use cases
export const ExtractionStatCard: React.FC<Omit<StatCardProps, 'format'>> = (props) => (
  <StatCard {...props} format="number" />
);

export const MigrationStatCard: React.FC<Omit<StatCardProps, 'format'>> = (props) => (
  <StatCard {...props} format="number" />
);

export const QualityStatCard: React.FC<Omit<StatCardProps, 'format'>> = (props) => (
  <StatCard {...props} format="percentage" />
);

export const ComplianceStatCard: React.FC<Omit<StatCardProps, 'format'>> = (props) => (
  <StatCard {...props} format="percentage" />
);

export default StatCard;