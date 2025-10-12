import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Chip,
  IconButton,
  Tabs,
  Tab,
  Button,
  Menu,
  MenuItem,
  Badge,
  Divider,
  Avatar,
} from '@mui/material';
import {
  Notifications as NotificationsIcon,
  CheckCircle as CheckCircleIcon,
  Error as ErrorIcon,
  Info as InfoIcon,
  Warning as WarningIcon,
  Delete as DeleteIcon,
  MoreVert as MoreVertIcon,
  MarkEmailRead as MarkReadIcon,
  DoneAll as DoneAllIcon,
} from '@mui/icons-material';

interface Notification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  timestamp: string;
  read: boolean;
  category: 'extraction' | 'migration' | 'quality' | 'compliance' | 'system';
}

const mockNotifications: Notification[] = [
  {
    id: '1',
    type: 'success',
    title: 'Migration Completed',
    message: 'Migration job #234 completed successfully with 10,000 records migrated.',
    timestamp: '5 minutes ago',
    read: false,
    category: 'migration',
  },
  {
    id: '2',
    type: 'error',
    title: 'Data Quality Check Failed',
    message: 'Data quality check failed for dataset "customers" - 12 validation errors found.',
    timestamp: '1 hour ago',
    read: false,
    category: 'quality',
  },
  {
    id: '3',
    type: 'info',
    title: 'New Compliance Request',
    message: 'New GDPR data subject request received (Request ID: DSR-2025-001).',
    timestamp: '2 hours ago',
    read: false,
    category: 'compliance',
  },
  {
    id: '4',
    type: 'success',
    title: 'Extraction Completed',
    message: 'Extraction job "Customer Data Extract" completed with 25,000 records.',
    timestamp: '3 hours ago',
    read: true,
    category: 'extraction',
  },
  {
    id: '5',
    type: 'warning',
    title: 'System Performance Alert',
    message: 'Database connection pool usage is at 85%. Consider scaling up resources.',
    timestamp: '5 hours ago',
    read: true,
    category: 'system',
  },
  {
    id: '6',
    type: 'info',
    title: 'Scheduled Maintenance',
    message: 'System maintenance scheduled for tonight at 2:00 AM UTC (2 hours).',
    timestamp: '1 day ago',
    read: true,
    category: 'system',
  },
];

const Notifications: React.FC = () => {
  const [notifications, setNotifications] = useState<Notification[]>(mockNotifications);
  const [tabValue, setTabValue] = useState(0);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedNotification, setSelectedNotification] = useState<string | null>(null);

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>, notificationId: string) => {
    setAnchorEl(event.currentTarget);
    setSelectedNotification(notificationId);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setSelectedNotification(null);
  };

  const markAsRead = (id: string) => {
    setNotifications(prev =>
      prev.map(n => (n.id === id ? { ...n, read: true } : n))
    );
    handleMenuClose();
  };

  const markAllAsRead = () => {
    setNotifications(prev => prev.map(n => ({ ...n, read: true })));
  };

  const deleteNotification = (id: string) => {
    setNotifications(prev => prev.filter(n => n.id !== id));
    handleMenuClose();
  };

  const getFilteredNotifications = () => {
    switch (tabValue) {
      case 0: // All
        return notifications;
      case 1: // Unread
        return notifications.filter(n => !n.read);
      case 2: // Extractions
        return notifications.filter(n => n.category === 'extraction');
      case 3: // Migrations
        return notifications.filter(n => n.category === 'migration');
      case 4: // Quality
        return notifications.filter(n => n.category === 'quality');
      default:
        return notifications;
    }
  };

  const getIcon = (type: string) => {
    switch (type) {
      case 'success':
        return <CheckCircleIcon color="success" />;
      case 'error':
        return <ErrorIcon color="error" />;
      case 'warning':
        return <WarningIcon color="warning" />;
      case 'info':
      default:
        return <InfoIcon color="info" />;
    }
  };

  const unreadCount = notifications.filter(n => !n.read).length;
  const filteredNotifications = getFilteredNotifications();

  return (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <NotificationsIcon sx={{ fontSize: 32 }} />
          <Box>
            <Typography variant="h4" component="h1" gutterBottom sx={{ mb: 0 }}>
              Notifications
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {unreadCount} unread notification{unreadCount !== 1 ? 's' : ''}
            </Typography>
          </Box>
        </Box>
        <Button
          variant="outlined"
          startIcon={<DoneAllIcon />}
          onClick={markAllAsRead}
          disabled={unreadCount === 0}
        >
          Mark All as Read
        </Button>
      </Box>

      {/* Tabs */}
      <Card sx={{ mb: 3 }}>
        <Tabs
          value={tabValue}
          onChange={handleTabChange}
          variant="scrollable"
          scrollButtons="auto"
          sx={{ borderBottom: 1, borderColor: 'divider' }}
        >
          <Tab
            label={
              <Badge badgeContent={notifications.length} color="primary" max={99}>
                All
              </Badge>
            }
          />
          <Tab
            label={
              <Badge badgeContent={unreadCount} color="error" max={99}>
                Unread
              </Badge>
            }
          />
          <Tab label="Extractions" />
          <Tab label="Migrations" />
          <Tab label="Quality" />
        </Tabs>
      </Card>

      {/* Notifications List */}
      <Card>
        <CardContent sx={{ p: 0 }}>
          {filteredNotifications.length === 0 ? (
            <Box sx={{ p: 4, textAlign: 'center' }}>
              <NotificationsIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary">
                No notifications
              </Typography>
              <Typography variant="body2" color="text.secondary">
                You're all caught up!
              </Typography>
            </Box>
          ) : (
            <List sx={{ p: 0 }}>
              {filteredNotifications.map((notification, index) => (
                <React.Fragment key={notification.id}>
                  <ListItem
                    sx={{
                      backgroundColor: notification.read ? 'transparent' : 'action.hover',
                      '&:hover': {
                        backgroundColor: 'action.selected',
                      },
                      alignItems: 'flex-start',
                      py: 2,
                    }}
                  >
                    <ListItemIcon sx={{ mt: 1 }}>
                      <Avatar
                        sx={{
                          bgcolor: notification.read ? 'grey.300' : 'primary.main',
                          width: 40,
                          height: 40,
                        }}
                      >
                        {getIcon(notification.type)}
                      </Avatar>
                    </ListItemIcon>
                    <ListItemText
                      primary={
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                          <Typography variant="subtitle1" sx={{ fontWeight: notification.read ? 400 : 600 }}>
                            {notification.title}
                          </Typography>
                          {!notification.read && (
                            <Chip label="New" color="primary" size="small" sx={{ height: 20 }} />
                          )}
                        </Box>
                      }
                      secondary={
                        <>
                          <Typography variant="body2" color="text.primary" sx={{ mb: 0.5 }}>
                            {notification.message}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            {notification.timestamp}
                          </Typography>
                        </>
                      }
                    />
                    <IconButton
                      edge="end"
                      onClick={(e) => handleMenuOpen(e, notification.id)}
                      size="small"
                    >
                      <MoreVertIcon />
                    </IconButton>
                  </ListItem>
                  {index < filteredNotifications.length - 1 && <Divider />}
                </React.Fragment>
              ))}
            </List>
          )}
        </CardContent>
      </Card>

      {/* Context Menu */}
      <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={handleMenuClose}>
        {selectedNotification && !notifications.find(n => n.id === selectedNotification)?.read && (
          <MenuItem onClick={() => selectedNotification && markAsRead(selectedNotification)}>
            <ListItemIcon>
              <MarkReadIcon fontSize="small" />
            </ListItemIcon>
            Mark as Read
          </MenuItem>
        )}
        <MenuItem onClick={() => selectedNotification && deleteNotification(selectedNotification)}>
          <ListItemIcon>
            <DeleteIcon fontSize="small" />
          </ListItemIcon>
          Delete
        </MenuItem>
      </Menu>
    </Box>
  );
};

export default Notifications;
