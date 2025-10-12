package com.jivs.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivs.platform.domain.UserPreferences;
import com.jivs.platform.dto.UserPreferencesDTO;
import com.jivs.platform.repository.UserPreferencesRepository;
import com.jivs.platform.security.UserPrincipal;
import com.jivs.platform.service.user.UserPreferencesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UserPreferencesController.
 * Tests the full flow from HTTP request through controller, service, repository to database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserPreferencesControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserPreferencesService preferencesService;

    @Autowired
    private UserPreferencesRepository repository;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        testUserId = 1L;

        // Set up security context with mock user
        UserPrincipal userPrincipal = UserPrincipal.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Clean up any existing preferences for test user
        repository.deleteByUserId(testUserId);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getThemePreference_WhenNotExists_ReturnsDefaultLight() throws Exception {
        mockMvc.perform(get("/api/v1/preferences/theme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme", is("light")));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getThemePreference_WhenExists_ReturnsStoredTheme() throws Exception {
        // Given: User has dark theme preference
        UserPreferences preferences = UserPreferences.builder()
                .userId(testUserId)
                .theme("dark")
                .language("en")
                .notificationsEnabled(true)
                .emailNotifications(true)
                .build();
        repository.save(preferences);

        // When: Get theme preference
        mockMvc.perform(get("/api/v1/preferences/theme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme", is("dark")));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void updateThemePreference_UpdatesSuccessfully() throws Exception {
        // Given: Theme update request
        UserPreferencesDTO dto = UserPreferencesDTO.builder()
                .theme("dark")
                .build();

        // When: Update theme
        mockMvc.perform(put("/api/v1/preferences/theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme", is("dark")));

        // Then: Verify theme is persisted in database
        UserPreferences savedPreferences = repository.findByUserId(testUserId).orElseThrow();
        assert savedPreferences.getTheme().equals("dark");
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void updateThemePreference_InvalidTheme_ReturnsBadRequest() throws Exception {
        // Given: Invalid theme value
        UserPreferencesDTO dto = UserPreferencesDTO.builder()
                .theme("invalid-theme")
                .build();

        // When: Update theme with invalid value
        mockMvc.perform(put("/api/v1/preferences/theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getAllPreferences_WhenNotExists_ReturnsDefaults() throws Exception {
        mockMvc.perform(get("/api/v1/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme", is("light")))
                .andExpect(jsonPath("$.language", is("en")))
                .andExpect(jsonPath("$.notificationsEnabled", is(true)))
                .andExpect(jsonPath("$.emailNotifications", is(true)));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getAllPreferences_WhenExists_ReturnsStoredPreferences() throws Exception {
        // Given: User has custom preferences
        UserPreferences preferences = UserPreferences.builder()
                .userId(testUserId)
                .theme("dark")
                .language("es")
                .notificationsEnabled(false)
                .emailNotifications(false)
                .build();
        repository.save(preferences);

        // When: Get all preferences
        mockMvc.perform(get("/api/v1/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme", is("dark")))
                .andExpect(jsonPath("$.language", is("es")))
                .andExpect(jsonPath("$.notificationsEnabled", is(false)))
                .andExpect(jsonPath("$.emailNotifications", is(false)));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void updatePreferences_UpdatesAllFields() throws Exception {
        // Given: Preferences update request
        UserPreferencesDTO dto = UserPreferencesDTO.builder()
                .theme("dark")
                .language("fr")
                .notificationsEnabled(false)
                .emailNotifications(false)
                .build();

        // When: Update all preferences
        mockMvc.perform(put("/api/v1/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme", is("dark")))
                .andExpect(jsonPath("$.language", is("fr")))
                .andExpect(jsonPath("$.notificationsEnabled", is(false)))
                .andExpect(jsonPath("$.emailNotifications", is(false)));

        // Then: Verify all fields are persisted
        UserPreferences savedPreferences = repository.findByUserId(testUserId).orElseThrow();
        assert savedPreferences.getTheme().equals("dark");
        assert savedPreferences.getLanguage().equals("fr");
        assert !savedPreferences.isNotificationsEnabled();
        assert !savedPreferences.isEmailNotifications();
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void updatePreferences_PartialUpdate_OnlyUpdatesProvidedFields() throws Exception {
        // Given: User has existing preferences
        UserPreferences existingPreferences = UserPreferences.builder()
                .userId(testUserId)
                .theme("light")
                .language("en")
                .notificationsEnabled(true)
                .emailNotifications(true)
                .build();
        repository.save(existingPreferences);

        // When: Partial update (only theme)
        UserPreferencesDTO dto = UserPreferencesDTO.builder()
                .theme("dark")
                .build();

        mockMvc.perform(put("/api/v1/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme", is("dark")))
                .andExpect(jsonPath("$.language", is("en"))) // Should remain unchanged
                .andExpect(jsonPath("$.notificationsEnabled", is(true))) // Should remain unchanged
                .andExpect(jsonPath("$.emailNotifications", is(true))); // Should remain unchanged
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void updatePreferences_InvalidLanguageFormat_ReturnsBadRequest() throws Exception {
        // Given: Invalid language code
        UserPreferencesDTO dto = UserPreferencesDTO.builder()
                .theme("light")
                .language("invalid-language-code")
                .build();

        // When: Update preferences with invalid language
        mockMvc.perform(put("/api/v1/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void themeSwitching_EndToEndFlow() throws Exception {
        // Step 1: Get initial theme (should be default "light")
        mockMvc.perform(get("/api/v1/preferences/theme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme", is("light")));

        // Step 2: Switch to dark theme
        UserPreferencesDTO darkTheme = UserPreferencesDTO.builder()
                .theme("dark")
                .build();

        mockMvc.perform(put("/api/v1/preferences/theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(darkTheme)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme", is("dark")));

        // Step 3: Verify theme is persisted (get again)
        mockMvc.perform(get("/api/v1/preferences/theme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme", is("dark")));

        // Step 4: Switch to auto theme
        UserPreferencesDTO autoTheme = UserPreferencesDTO.builder()
                .theme("auto")
                .build();

        mockMvc.perform(put("/api/v1/preferences/theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(autoTheme)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme", is("auto")));

        // Step 5: Verify auto theme persists
        mockMvc.perform(get("/api/v1/preferences/theme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme", is("auto")));
    }
}
