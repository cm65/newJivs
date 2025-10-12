# Scenario 3: Dark Mode UI Feature

## Overview
Add dark mode theme support to JiVS platform UI, allowing users to toggle between light and dark themes with preference persistence.

## Business Requirements
- **Priority**: P2 (Nice-to-Have / User Experience)
- **Sprint**: Sprint 47
- **Estimated Effort**: 3 days
- **Target Date**: Mid Sprint 47

## Feature Description
Implement a dark mode theme toggle that:
1. Provides a Material-UI dark theme
2. Allows users to toggle between light/dark modes
3. Persists user preference (localStorage + backend)
4. Supports all existing components
5. Includes smooth theme transitions
6. Defaults to system preference (prefers-color-scheme)

## User Stories

### Story 1: Theme Toggle
**As a** JiVS user
**I want to** toggle between light and dark modes
**So that** I can work comfortably in different lighting conditions

**Acceptance Criteria**:
- Toggle button in app bar (sun/moon icon)
- Smooth transition animation (300ms)
- Theme persists across sessions

### Story 2: System Preference Detection
**As a** JiVS user
**I want** the app to respect my system theme preference
**So that** I don't have to manually set my preferred theme

**Acceptance Criteria**:
- Detects system preference on first load
- Allows manual override
- Remembers manual override

### Story 3: Component Compatibility
**As a** developer
**I want** all UI components to support dark mode
**So that** the experience is consistent

**Acceptance Criteria**:
- All pages render correctly in dark mode
- Charts and graphs have dark variants
- Form inputs are readable in dark mode

## Technical Requirements

### Frontend Components

#### 1. Theme Configuration
```typescript
// theme/index.ts
import { createTheme } from '@mui/material/styles';

export const lightTheme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
    background: {
      default: '#f5f5f5',
      paper: '#ffffff',
    },
  },
});

export const darkTheme = createTheme({
  palette: {
    mode: 'dark',
    primary: {
      main: '#90caf9',
    },
    secondary: {
      main: '#f48fb1',
    },
    background: {
      default: '#121212',
      paper: '#1e1e1e',
    },
  },
});
```

#### 2. Theme Context Provider
```typescript
// contexts/ThemeContext.tsx
import React, { createContext, useState, useEffect, useMemo } from 'react';
import { ThemeProvider } from '@mui/material/styles';
import { lightTheme, darkTheme } from '../theme';

interface ThemeContextType {
  mode: 'light' | 'dark';
  toggleTheme: () => void;
}

export const ThemeContext = createContext<ThemeContextType>({
  mode: 'light',
  toggleTheme: () => {},
});

export const ThemeContextProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [mode, setMode] = useState<'light' | 'dark'>('light');

  useEffect(() => {
    // Check localStorage
    const savedMode = localStorage.getItem('theme-mode') as 'light' | 'dark' | null;

    if (savedMode) {
      setMode(savedMode);
    } else {
      // Check system preference
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      setMode(prefersDark ? 'dark' : 'light');
    }
  }, []);

  const toggleTheme = () => {
    setMode((prevMode) => {
      const newMode = prevMode === 'light' ? 'dark' : 'light';
      localStorage.setItem('theme-mode', newMode);
      return newMode;
    });
  };

  const theme = useMemo(() => (mode === 'light' ? lightTheme : darkTheme), [mode]);

  return (
    <ThemeContext.Provider value={{ mode, toggleTheme }}>
      <ThemeProvider theme={theme}>{children}</ThemeProvider>
    </ThemeContext.Provider>
  );
};
```

#### 3. Theme Toggle Button
```typescript
// components/ThemeToggle.tsx
import React, { useContext } from 'react';
import { IconButton, Tooltip } from '@mui/material';
import { Brightness4, Brightness7 } from '@mui/icons-material';
import { ThemeContext } from '../contexts/ThemeContext';

export const ThemeToggle: React.FC = () => {
  const { mode, toggleTheme } = useContext(ThemeContext);

  return (
    <Tooltip title={`Switch to ${mode === 'light' ? 'dark' : 'light'} mode`}>
      <IconButton onClick={toggleTheme} color="inherit">
        {mode === 'light' ? <Brightness4 /> : <Brightness7 />}
      </IconButton>
    </Tooltip>
  );
};
```

#### 4. Chart Theme Support (Recharts)
```typescript
// components/charts/PerformanceChart.tsx
import { useContext } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend } from 'recharts';
import { ThemeContext } from '../../contexts/ThemeContext';

export const PerformanceChart: React.FC<{ data: any[] }> = ({ data }) => {
  const { mode } = useContext(ThemeContext);

  const colors = mode === 'dark'
    ? { grid: '#333', text: '#ccc', line: '#90caf9' }
    : { grid: '#ccc', text: '#333', line: '#1976d2' };

  return (
    <LineChart width={600} height={300} data={data}>
      <CartesianGrid strokeDasharray="3 3" stroke={colors.grid} />
      <XAxis dataKey="name" stroke={colors.text} />
      <YAxis stroke={colors.text} />
      <Tooltip contentStyle={{ backgroundColor: mode === 'dark' ? '#1e1e1e' : '#fff' }} />
      <Legend />
      <Line type="monotone" dataKey="value" stroke={colors.line} />
    </LineChart>
  );
};
```

### Backend Components

#### 1. User Preference Storage
```java
// domain/UserPreferences.java
@Entity
@Table(name = "user_preferences")
public class UserPreferences {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    @Column(name = "theme_mode", length = 10)
    private String themeMode; // "light" or "dark"

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

#### 2. Preferences API
```java
// controller/UserPreferencesController.java
@RestController
@RequestMapping("/api/v1/preferences")
public class UserPreferencesController {

    @Autowired
    private UserPreferencesService preferencesService;

    @GetMapping("/theme")
    public ResponseEntity<ThemePreferenceResponse> getThemePreference() {
        Long userId = getCurrentUserId();
        UserPreferences prefs = preferencesService.getPreferences(userId);
        return ResponseEntity.ok(new ThemePreferenceResponse(prefs.getThemeMode()));
    }

    @PutMapping("/theme")
    public ResponseEntity<Void> updateThemePreference(@RequestBody ThemePreferenceRequest request) {
        Long userId = getCurrentUserId();
        preferencesService.updateThemeMode(userId, request.getThemeMode());
        return ResponseEntity.ok().build();
    }
}
```

#### 3. Flyway Migration
```sql
-- V47__Create_User_Preferences_Table.sql
CREATE TABLE user_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    theme_mode VARCHAR(10) DEFAULT 'light',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_preferences_user_id ON user_preferences(user_id);
```

## Acceptance Criteria
1. ✅ Theme toggle button in app bar (all pages)
2. ✅ Light and dark themes defined with Material-UI
3. ✅ All pages render correctly in both modes
4. ✅ Charts support dark mode colors
5. ✅ Theme preference persists in localStorage
6. ✅ Theme preference syncs to backend
7. ✅ System preference detection on first load
8. ✅ Smooth transition animation (300ms)
9. ✅ All form inputs readable in dark mode
10. ✅ All status indicators visible in dark mode
11. ✅ Unit tests for theme context
12. ✅ E2E tests for theme toggle

## Workflow Execution

### Phase 1: Planning (jivs-sprint-prioritizer)
**Expected Outputs**:
- Sprint plan with P2 priority
- 3-day effort estimate
- Risk assessment (LOW - UI only, no business logic impact)

**Execution Mode**: `development` (planning, design, testing only)

### Phase 2: Design (jivs-frontend-developer, jivs-backend-architect)

**Frontend Design (jivs-frontend-developer)**:
- Theme configuration (light + dark)
- ThemeContext provider
- ThemeToggle component
- Update all pages to use theme
- Chart theme variants

**Backend Design (jivs-backend-architect)**:
- UserPreferences entity
- PreferencesService
- REST API for theme preference
- Flyway migration

### Phase 3: Testing (jivs-test-writer-fixer, jivs-test-results-analyzer)

**Unit Tests**:
```typescript
// ThemeContext.test.tsx
describe('ThemeContext', () => {
  it('should default to system preference', () => {
    // Test prefers-color-scheme detection
  });

  it('should toggle theme', () => {
    // Test theme toggle
  });

  it('should persist to localStorage', () => {
    // Test localStorage persistence
  });
});
```

**E2E Tests**:
```typescript
// dark-mode.spec.ts
test('Dark mode toggle', async ({ page }) => {
  await page.goto('http://localhost:3000/dashboard');

  // Toggle dark mode
  await page.click('[data-testid="theme-toggle"]');

  // Verify dark theme applied
  const bgColor = await page.evaluate(() => {
    return window.getComputedStyle(document.body).backgroundColor;
  });
  expect(bgColor).toBe('rgb(18, 18, 18)'); // #121212

  // Verify persistence
  await page.reload();
  const bgColorAfterReload = await page.evaluate(() => {
    return window.getComputedStyle(document.body).backgroundColor;
  });
  expect(bgColorAfterReload).toBe('rgb(18, 18, 18)');
});
```

**Visual Regression Tests**:
```typescript
// visual-tests/dark-mode.spec.ts
test('Dashboard dark mode screenshot', async ({ page }) => {
  await page.goto('http://localhost:3000/dashboard');
  await page.click('[data-testid="theme-toggle"]');
  await expect(page).toHaveScreenshot('dashboard-dark.png');
});
```

## Quality Gates

### Testing Phase Gate
- All pages render correctly: ✅ YES
- Theme toggle works: ✅ YES
- Persistence works: ✅ YES
- Unit tests pass: ✅ 100%
- E2E tests pass: ✅ 100%
- Visual regression tests pass: ✅ YES

## Pages to Update

### Core Pages
1. ✅ Dashboard
2. ✅ Extractions
3. ✅ Migrations
4. ✅ Data Quality
5. ✅ Compliance
6. ✅ Analytics
7. ✅ Settings

### Components to Update
1. ✅ Layout (app bar, drawer)
2. ✅ Data tables
3. ✅ Forms (all inputs)
4. ✅ Cards
5. ✅ Charts (Recharts)
6. ✅ Dialogs
7. ✅ Alerts
8. ✅ Status badges

## Design System Updates

### Color Palette

**Light Theme**:
- Primary: #1976d2 (blue)
- Secondary: #dc004e (pink)
- Background: #f5f5f5
- Paper: #ffffff
- Text Primary: rgba(0, 0, 0, 0.87)
- Text Secondary: rgba(0, 0, 0, 0.54)

**Dark Theme**:
- Primary: #90caf9 (light blue)
- Secondary: #f48fb1 (light pink)
- Background: #121212
- Paper: #1e1e1e
- Text Primary: rgba(255, 255, 255, 0.87)
- Text Secondary: rgba(255, 255, 255, 0.54)

## Success Metrics
- **Feature Delivery**: On time (3 days)
- **User Adoption**: 30% of users enable dark mode within 1 week
- **User Feedback**: Positive feedback on theme quality
- **No Regressions**: All existing functionality works

## Risks & Mitigations

### Low Risks
1. **Component styling issues in dark mode**
   - Mitigation: Comprehensive visual testing, manual QA

2. **Chart readability in dark mode**
   - Mitigation: User feedback, A/B testing of color palettes

## Future Enhancements
1. Additional theme variants (high contrast, colorblind-friendly)
2. Scheduled theme switching (auto dark mode at night)
3. Per-page theme overrides
4. Theme customization (user-defined colors)

## Workflow Execution Command

```bash
# Execute development workflow (planning, design, testing)
./workflow-orchestrator.sh --mode development --scenario "Dark Mode UI Feature"

# Expected duration: ~1 hour
# Phases executed: Planning (1 agent), Design (2 agents), Testing (2 agents)
```

## Manual Testing Checklist

- [ ] Toggle dark mode from app bar
- [ ] Verify theme persists after page reload
- [ ] Verify theme persists after logout/login
- [ ] Test all pages in dark mode
- [ ] Test all forms in dark mode
- [ ] Test all charts in dark mode
- [ ] Test theme toggle animation
- [ ] Test on Chrome, Firefox, Safari
- [ ] Test on mobile devices
- [ ] Verify accessibility (contrast ratios)

## Accessibility Considerations
- **Contrast Ratios**: All text meets WCAG AA standards (4.5:1)
- **Focus Indicators**: Visible in both themes
- **Color Alone**: Never used as the only indicator
- **Keyboard Navigation**: Works in both themes

## Documentation Updates
1. Update user guide with theme toggle instructions
2. Update developer guide with theme customization
3. Add screenshots of dark mode to marketing materials
4. Update component library with dark mode examples
