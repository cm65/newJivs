import React, { useState } from 'react';
import { Outlet, useNavigate, useLocation, Link as RouterLink } from 'react-router-dom';
import {
  AppBar,
  Box,
  CssBaseline,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
  Avatar,
  Menu,
  MenuItem,
  Breadcrumbs,
  Link,
  Chip,
  Badge,
  Tooltip,
  useTheme,
} from '@mui/material';
import {
  Menu as MenuIcon,
  Dashboard as DashboardIcon,
  Storage as StorageIcon,
  Transform as TransformIcon,
  BusinessCenter as BusinessIcon,
  HighQuality as QualityIcon,
  Policy as PolicyIcon,
  Description as DocumentIcon,
  People as PeopleIcon,
  Settings as SettingsIcon,
  ExitToApp as LogoutIcon,
  AccountCircle,
  NavigateNext as NavigateNextIcon,
  Notifications as NotificationsIcon,
  Home as HomeIcon,
} from '@mui/icons-material';
import { useAuth } from '../contexts/AuthContext';
import { visuallyHidden } from '../styles/theme';
import QuickActions from '../components/QuickActions';
import ThemeToggle from '../components/ThemeToggle';

const drawerWidth = 240;

interface MenuItem {
  text: string;
  icon: React.ReactNode;
  path: string;
  parent?: string;
  roles?: string[];
}

const menuItems: MenuItem[] = [
  { text: 'Dashboard', icon: <DashboardIcon />, path: '/dashboard' },
  { text: 'Extractions', icon: <StorageIcon />, path: '/extractions' },
  { text: 'Migrations', icon: <TransformIcon />, path: '/migrations' },
  { text: 'Business Objects', icon: <BusinessIcon />, path: '/business-objects' },
  { text: 'Data Quality', icon: <QualityIcon />, path: '/data-quality' },
  { text: 'Compliance', icon: <PolicyIcon />, path: '/compliance', roles: ['ADMIN', 'COMPLIANCE_OFFICER'] },
  { text: 'Documents', icon: <DocumentIcon />, path: '/documents' },
  { text: 'Users', icon: <PeopleIcon />, path: '/users', roles: ['ADMIN'] },
  { text: 'Settings', icon: <SettingsIcon />, path: '/settings' },
];

// Breadcrumb mapping for nested routes
const breadcrumbNameMap: { [key: string]: string } = {
  '/dashboard': 'Dashboard',
  '/extractions': 'Extractions',
  '/extractions/new': 'New Extraction',
  '/migrations': 'Migrations',
  '/migrations/new': 'New Migration',
  '/business-objects': 'Business Objects',
  '/data-quality': 'Data Quality',
  '/data-quality/rules': 'Quality Rules',
  '/compliance': 'Compliance',
  '/compliance/requests': 'Data Subject Requests',
  '/documents': 'Documents',
  '/users': 'User Management',
  '/settings': 'Settings',
  '/settings/profile': 'Profile',
  '/settings/security': 'Security',
};

const MainLayout: React.FC = () => {
  const theme = useTheme();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [notificationAnchor, setNotificationAnchor] = useState<null | HTMLElement>(null);
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuth();

  // Generate breadcrumbs based on current path
  const pathnames = location.pathname.split('/').filter(x => x);

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleNotificationOpen = (event: React.MouseEvent<HTMLElement>) => {
    setNotificationAnchor(event.currentTarget);
  };

  const handleNotificationClose = () => {
    setNotificationAnchor(null);
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  // Check if user has required role for menu item
  const hasRole = (requiredRoles?: string[]) => {
    if (!requiredRoles) return true;
    return requiredRoles.some(role => user?.roles?.includes(role));
  };

  const drawer = (
    <div>
      <Toolbar>
        <Typography
          variant="h6"
          noWrap
          component="div"
          sx={{
            fontWeight: 'bold',
            background: theme.palette.mode === 'light'
              ? 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
              : 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
          }}
        >
          JiVS Platform
        </Typography>
      </Toolbar>
      <Divider />
      <List>
        {menuItems.filter(item => hasRole(item.roles)).map((item) => (
          <ListItem key={item.text} disablePadding>
            <ListItemButton
              selected={location.pathname.startsWith(item.path)}
              onClick={() => navigate(item.path)}
              sx={{
                '&.Mui-selected': {
                  backgroundColor: theme.palette.primary.main,
                  '& .MuiListItemIcon-root': {
                    color: 'white',
                  },
                  '& .MuiListItemText-primary': {
                    color: 'white',
                  },
                  '&:hover': {
                    backgroundColor: theme.palette.primary.dark,
                  },
                },
                '&:focus-visible': {
                  outline: '2px solid',
                  outlineColor: theme.palette.primary.main,
                  outlineOffset: -2,
                },
              }}
            >
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
      <Divider />
      <Box sx={{ p: 2, mt: 'auto' }}>
        <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1 }}>
          Version 1.0.0
        </Typography>
        <Typography variant="caption" color="text.secondary" display="block">
          © 2025 JiVS Platform
        </Typography>
      </Box>
    </div>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      <CssBaseline />

      {/* Skip to main content link for accessibility */}
      <Link
        component="button"
        onClick={() => {
          document.getElementById('main-content')?.focus();
        }}
        sx={{
          ...visuallyHidden,
          '&:focus': {
            position: 'fixed',
            top: 0,
            left: 0,
            zIndex: 9999,
            padding: 2,
            backgroundColor: 'background.paper',
            ...theme.typography.body1,
          },
        }}
      >
        Skip to main content
      </Link>

      <AppBar
        position="fixed"
        sx={{
          width: { sm: `calc(100% - ${drawerWidth}px)` },
          ml: { sm: `${drawerWidth}px` },
          boxShadow: 1,
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="open drawer"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ mr: 2, display: { sm: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1, fontWeight: 600 }}>
            {breadcrumbNameMap[location.pathname] || 'JiVS Platform'}
          </Typography>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            {/* Theme Toggle */}
            <ThemeToggle color="inherit" />

            {/* Notifications */}
            <Tooltip title="Notifications">
              <IconButton onClick={handleNotificationOpen} color="inherit">
                <Badge badgeContent={3} color="error">
                  <NotificationsIcon />
                </Badge>
              </IconButton>
            </Tooltip>

            {/* User Menu */}
            <Chip
              avatar={
                <Avatar sx={{ width: 28, height: 28, fontSize: '0.875rem' }}>
                  {user?.username?.charAt(0).toUpperCase()}
                </Avatar>
              }
              label={user?.username}
              onClick={handleMenuOpen}
              sx={{
                color: 'white',
                backgroundColor: 'rgba(255, 255, 255, 0.15)',
                '&:hover': {
                  backgroundColor: 'rgba(255, 255, 255, 0.25)',
                },
                cursor: 'pointer',
              }}
            />
          </Box>

          <Menu
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleMenuClose}
            anchorOrigin={{
              vertical: 'bottom',
              horizontal: 'right',
            }}
            transformOrigin={{
              vertical: 'top',
              horizontal: 'right',
            }}
          >
            <MenuItem onClick={() => { navigate('/settings/profile'); handleMenuClose(); }}>
              <ListItemIcon><AccountCircle fontSize="small" /></ListItemIcon>
              Profile
            </MenuItem>
            <MenuItem onClick={() => { navigate('/settings'); handleMenuClose(); }}>
              <ListItemIcon><SettingsIcon fontSize="small" /></ListItemIcon>
              Settings
            </MenuItem>
            <Divider />
            <MenuItem onClick={handleLogout}>
              <ListItemIcon><LogoutIcon fontSize="small" /></ListItemIcon>
              Logout
            </MenuItem>
          </Menu>

          {/* Notifications Menu */}
          <Menu
            anchorEl={notificationAnchor}
            open={Boolean(notificationAnchor)}
            onClose={handleNotificationClose}
            anchorOrigin={{
              vertical: 'bottom',
              horizontal: 'right',
            }}
            transformOrigin={{
              vertical: 'top',
              horizontal: 'right',
            }}
            PaperProps={{
              sx: { width: 320, maxHeight: 400 },
            }}
          >
            <Box sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>
                Notifications
              </Typography>
            </Box>
            <Divider />
            <MenuItem onClick={handleNotificationClose}>
              <Box>
                <Typography variant="body2">
                  Migration job #234 completed successfully
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  5 minutes ago
                </Typography>
              </Box>
            </MenuItem>
            <MenuItem onClick={handleNotificationClose}>
              <Box>
                <Typography variant="body2">
                  Data quality check failed for dataset "customers"
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  1 hour ago
                </Typography>
              </Box>
            </MenuItem>
            <MenuItem onClick={handleNotificationClose}>
              <Box>
                <Typography variant="body2">
                  New compliance request received
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  2 hours ago
                </Typography>
              </Box>
            </MenuItem>
            <Divider />
            <MenuItem onClick={() => { navigate('/notifications'); handleNotificationClose(); }}>
              <Typography variant="body2" color="primary" sx={{ textAlign: 'center', width: '100%' }}>
                View all notifications
              </Typography>
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Box
        component="nav"
        sx={{ width: { sm: drawerWidth }, flexShrink: { sm: 0 } }}
        aria-label="main navigation"
      >
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: 'block', sm: 'none' },
            '& .MuiDrawer-paper': {
              boxSizing: 'border-box',
              width: drawerWidth,
            },
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', sm: 'block' },
            '& .MuiDrawer-paper': {
              boxSizing: 'border-box',
              width: drawerWidth,
              borderRight: `1px solid ${theme.palette.divider}`,
            },
          }}
          open
        >
          {drawer}
        </Drawer>
      </Box>

      <Box
        component="main"
        id="main-content"
        tabIndex={-1}
        sx={{
          flexGrow: 1,
          width: { sm: `calc(100% - ${drawerWidth}px)` },
          mt: '64px', // AppBar height
          minHeight: 'calc(100vh - 64px)',
        }}
      >
        {/* Breadcrumbs */}
        {pathnames.length > 0 && (
          <Box
            sx={{
              p: 2,
              pb: 0,
              backgroundColor: 'background.default',
            }}
          >
            <Breadcrumbs
              separator={<NavigateNextIcon fontSize="small" />}
              aria-label="breadcrumb"
              sx={{
                '& .MuiBreadcrumbs-separator': {
                  color: 'text.secondary',
                },
              }}
            >
              <Link
                component={RouterLink}
                to="/dashboard"
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  color: 'text.secondary',
                  textDecoration: 'none',
                  '&:hover': {
                    color: 'primary.main',
                    textDecoration: 'underline',
                  },
                  '&:focus-visible': {
                    outline: '2px solid',
                    outlineColor: theme.palette.primary.main,
                    outlineOffset: 2,
                    borderRadius: 1,
                  },
                }}
              >
                <HomeIcon sx={{ mr: 0.5, fontSize: 20 }} />
                Home
              </Link>

              {pathnames.map((value, index) => {
                const last = index === pathnames.length - 1;
                const to = `/${pathnames.slice(0, index + 1).join('/')}`;
                const breadcrumbName = breadcrumbNameMap[to] || value;

                return last ? (
                  <Typography color="text.primary" key={to} sx={{ fontWeight: 500 }}>
                    {breadcrumbName}
                  </Typography>
                ) : (
                  <Link
                    component={RouterLink}
                    to={to}
                    key={to}
                    sx={{
                      color: 'text.secondary',
                      textDecoration: 'none',
                      '&:hover': {
                        color: 'primary.main',
                        textDecoration: 'underline',
                      },
                      '&:focus-visible': {
                        outline: '2px solid',
                        outlineColor: theme.palette.primary.main,
                        outlineOffset: 2,
                        borderRadius: 1,
                      },
                    }}
                  >
                    {breadcrumbName}
                  </Link>
                );
              })}
            </Breadcrumbs>
          </Box>
        )}

        {/* Page Content */}
        <Box sx={{ p: 3 }}>
          <Outlet />
        </Box>

        {/* Quick Actions Menu */}
        <QuickActions />
      </Box>
    </Box>
  );
};

export default MainLayout;
