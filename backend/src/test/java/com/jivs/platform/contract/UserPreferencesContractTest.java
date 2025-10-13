package com.jivs.platform.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.jivs.platform.domain.preferences.UserPreferences;
import com.jivs.platform.domain.preferences.NotificationSettings;
import com.jivs.platform.domain.preferences.DisplaySettings;
import com.jivs.platform.domain.preferences.DashboardConfig;
import com.jivs.platform.domain.preferences.DataTablePreferences;
import com.jivs.platform.domain.preferences.ExportPreferences;
import com.jivs.platform.domain.preferences.WidgetLayout;
import com.jivs.platform.repository.UserPreferencesRepository;
import com.jivs.platform.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * User Preferences Contract Test - Provider Side
 *
 * This test verifies that the backend user preferences endpoints
 * satisfy the contracts defined by the frontend.
 *
 * User Preferences are important for:
 * - Personalized user experience
 * - UI customization (theme, language, timezone)
 * - Notification management
 * - Dashboard configuration
 * - Export settings
 *
 * Coverage: 4/4 User Preferences endpoints
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("JiVS Backend")
@PactFolder("../frontend/pacts")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class UserPreferencesContractTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserPreferencesRepository preferencesRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setup(PactVerificationContext context) {
        // Configure the test to hit our running Spring Boot application
        context.setTarget(new HttpTestTarget("localhost", port));

        // Mock JWT validation for tests
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(1L);
        when(jwtTokenProvider.getRolesFromToken(anyString())).thenReturn(Collections.singletonList("ROLE_USER"));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        // This will run once for each interaction in the pact file
        context.verifyInteraction();
    }

    /**
     * Provider States - Set up test data for different user preferences scenarios
     */

    @State("user is authenticated")
    public void userIsAuthenticated() {
        System.out.println("Setting up: User is authenticated");
        // Authentication is mocked in setup()
    }

    @State("user is authenticated and has preferences")
    public void userAuthenticatedWithPreferences() {
        System.out.println("Setting up: User is authenticated and has preferences");

        // Clear existing data
        preferencesRepository.deleteAll();

        // Create user preferences
        UserPreferences preferences = new UserPreferences();
        preferences.setId(UUID.randomUUID().toString());
        preferences.setUserId("USER-001");
        preferences.setTheme("light");
        preferences.setLanguage("en-US");
        preferences.setTimezone("America/New_York");

        // Notification settings
        NotificationSettings notifications = new NotificationSettings();
        notifications.setEmail(true);
        notifications.setSms(false);
        notifications.setPush(true);
        notifications.setInApp(true);
        preferences.setNotifications(notifications);

        // Display settings
        DisplaySettings displaySettings = new DisplaySettings();
        displaySettings.setDateFormat("MM/DD/YYYY");
        displaySettings.setTimeFormat("12h");
        displaySettings.setNumberFormat("1,234.56");
        displaySettings.setPageSize("20");
        preferences.setDisplaySettings(displaySettings);

        // Dashboard configuration
        DashboardConfig dashboardConfig = new DashboardConfig();
        dashboardConfig.setDefaultView("grid");
        dashboardConfig.setRefreshInterval("60");

        List<WidgetLayout> widgetLayouts = new ArrayList<>();
        String[] widgetIds = {"extraction-stats", "migration-progress", "quality-score", "compliance-rate"};
        for (int i = 0; i < 4; i++) {
            WidgetLayout widget = new WidgetLayout();
            widget.setWidgetId(widgetIds[i]);
            widget.setPosition(String.format("{\"x\":\"%d\",\"y\":\"%d\",\"w\":\"4\",\"h\":\"2\"}",
                                            (i % 2) * 4, (i / 2) * 2));
            widget.setVisible(true);
            widgetLayouts.add(widget);
        }
        dashboardConfig.setWidgetLayout(widgetLayouts);
        preferences.setDashboardConfig(dashboardConfig);

        // Data table preferences
        DataTablePreferences tablePrefs = new DataTablePreferences();
        tablePrefs.setCompactMode(false);
        tablePrefs.setShowFilters(true);
        tablePrefs.setStickyHeader(true);
        tablePrefs.setHighlightRows(true);
        preferences.setDataTablePreferences(tablePrefs);

        // Export preferences
        ExportPreferences exportPrefs = new ExportPreferences();
        exportPrefs.setDefaultFormat("xlsx");
        exportPrefs.setIncludeHeaders(true);
        exportPrefs.setIncludeMetadata(false);
        exportPrefs.setCompression(true);
        preferences.setExportPreferences(exportPrefs);

        preferences.setCreatedAt(LocalDateTime.now().minusDays(30));
        preferences.setUpdatedAt(LocalDateTime.now().minusDays(1));

        preferencesRepository.save(preferences);
    }

    @State("user is authenticated and can update preferences")
    public void userAuthenticatedCanUpdatePreferences() {
        System.out.println("Setting up: User is authenticated and can update preferences");

        // Clear existing data
        preferencesRepository.deleteAll();

        // Create existing preferences that will be updated
        UserPreferences preferences = new UserPreferences();
        preferences.setId(UUID.randomUUID().toString());
        preferences.setUserId("USER-001");
        preferences.setTheme("light");
        preferences.setLanguage("en-US");
        preferences.setTimezone("America/New_York");

        NotificationSettings notifications = new NotificationSettings();
        notifications.setEmail(true);
        notifications.setSms(false);
        notifications.setPush(false);
        notifications.setInApp(true);
        preferences.setNotifications(notifications);

        DisplaySettings displaySettings = new DisplaySettings();
        displaySettings.setDateFormat("MM/DD/YYYY");
        displaySettings.setTimeFormat("12h");
        displaySettings.setNumberFormat("1,234.56");
        displaySettings.setPageSize("20");
        preferences.setDisplaySettings(displaySettings);

        preferences.setCreatedAt(LocalDateTime.now().minusDays(15));
        preferences.setUpdatedAt(LocalDateTime.now().minusDays(2));

        preferencesRepository.save(preferences);
    }

    @State("user is authenticated and can reset preferences")
    public void userAuthenticatedCanResetPreferences() {
        System.out.println("Setting up: User is authenticated and can reset preferences");

        // Clear existing data
        preferencesRepository.deleteAll();

        // Create customized preferences that will be reset
        UserPreferences preferences = new UserPreferences();
        preferences.setId(UUID.randomUUID().toString());
        preferences.setUserId("USER-001");
        preferences.setTheme("dark");
        preferences.setLanguage("fr-FR");
        preferences.setTimezone("Europe/Paris");

        NotificationSettings notifications = new NotificationSettings();
        notifications.setEmail(false);
        notifications.setSms(true);
        notifications.setPush(true);
        notifications.setInApp(false);
        preferences.setNotifications(notifications);

        DisplaySettings displaySettings = new DisplaySettings();
        displaySettings.setDateFormat("DD/MM/YYYY");
        displaySettings.setTimeFormat("24h");
        displaySettings.setNumberFormat("1.234,56");
        displaySettings.setPageSize("100");
        preferences.setDisplaySettings(displaySettings);

        preferences.setCreatedAt(LocalDateTime.now().minusDays(60));
        preferences.setUpdatedAt(LocalDateTime.now().minusDays(5));

        preferencesRepository.save(preferences);
    }

    @State("user is authenticated and can export preferences")
    public void userAuthenticatedCanExportPreferences() {
        System.out.println("Setting up: User is authenticated and can export preferences");

        // Clear existing data
        preferencesRepository.deleteAll();

        // Create complete preferences for export
        UserPreferences preferences = new UserPreferences();
        preferences.setId(UUID.randomUUID().toString());
        preferences.setUserId("USER-001");
        preferences.setTheme("dark");
        preferences.setLanguage("en-US");
        preferences.setTimezone("America/New_York");

        NotificationSettings notifications = new NotificationSettings();
        notifications.setEmail(true);
        notifications.setSms(false);
        notifications.setPush(true);
        notifications.setInApp(true);
        preferences.setNotifications(notifications);

        DisplaySettings displaySettings = new DisplaySettings();
        displaySettings.setDateFormat("MM/DD/YYYY");
        displaySettings.setTimeFormat("12h");
        displaySettings.setNumberFormat("1,234.56");
        displaySettings.setPageSize("20");
        preferences.setDisplaySettings(displaySettings);

        DashboardConfig dashboardConfig = new DashboardConfig();
        dashboardConfig.setDefaultView("grid");
        dashboardConfig.setRefreshInterval("60");
        preferences.setDashboardConfig(dashboardConfig);

        DataTablePreferences tablePrefs = new DataTablePreferences();
        tablePrefs.setCompactMode(false);
        tablePrefs.setShowFilters(true);
        preferences.setDataTablePreferences(tablePrefs);

        ExportPreferences exportPrefs = new ExportPreferences();
        exportPrefs.setDefaultFormat("xlsx");
        exportPrefs.setIncludeHeaders(true);
        preferences.setExportPreferences(exportPrefs);

        preferences.setCreatedAt(LocalDateTime.now().minusDays(90));
        preferences.setUpdatedAt(LocalDateTime.now());

        preferencesRepository.save(preferences);
    }
}

/**
 * WHY USER PREFERENCES CONTRACT TESTS MATTER:
 *
 * 1. User experience personalization must be consistent
 * 2. Theme changes affect the entire application UI
 * 3. Timezone settings impact date/time display globally
 * 4. Notification preferences control critical communications
 * 5. Export settings affect all data exports
 *
 * These tests ensure:
 * - Frontend and backend agree on preference structure
 * - Default values are consistent
 * - Updates persist correctly
 * - Reset returns to proper defaults
 * - Export format is portable
 *
 * Benefits:
 * - Catch preference structure mismatches
 * - Ensure consistent UI customization
 * - Validate notification delivery settings
 * - Confirm export compatibility
 * - Maintain user experience quality
 */