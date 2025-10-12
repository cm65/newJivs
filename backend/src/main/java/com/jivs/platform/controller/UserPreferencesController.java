package com.jivs.platform.controller;

import com.jivs.platform.domain.UserPreferences;
import com.jivs.platform.dto.UserPreferencesDTO;
import com.jivs.platform.security.JwtTokenProvider;
import com.jivs.platform.service.UserPreferencesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * REST controller for user preferences management
 * Handles theme preferences, notification settings, and other user-specific configurations
 */
@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Preferences", description = "User preferences management API")
@SecurityRequirement(name = "bearerAuth")
public class UserPreferencesController {

    private final UserPreferencesService preferencesService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Get user's theme preference
     * @param request HTTP request to extract user ID from JWT
     * @return User preferences with theme setting
     */
    @GetMapping("/theme")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get theme preference", description = "Get user's theme preference (light/dark)")
    public ResponseEntity<UserPreferencesDTO> getThemePreference(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        log.debug("Getting theme preference for user: {}", userId);

        UserPreferences preferences = preferencesService.getUserPreferences(userId);
        UserPreferencesDTO dto = new UserPreferencesDTO();
        dto.setTheme(preferences.getTheme());

        return ResponseEntity.ok(dto);
    }

    /**
     * Update user's theme preference
     * @param request HTTP request to extract user ID from JWT
     * @param dto Theme preference DTO
     * @return Updated preferences
     */
    @PutMapping("/theme")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update theme preference", description = "Update user's theme preference (light/dark)")
    public ResponseEntity<UserPreferencesDTO> updateThemePreference(
            HttpServletRequest request,
            @Valid @RequestBody UserPreferencesDTO dto) {
        Long userId = getUserIdFromRequest(request);
        log.info("Updating theme preference for user {} to: {}", userId, dto.getTheme());

        UserPreferences preferences = preferencesService.updateTheme(userId, dto.getTheme());
        UserPreferencesDTO responseDto = new UserPreferencesDTO();
        responseDto.setTheme(preferences.getTheme());

        return ResponseEntity.ok(responseDto);
    }

    /**
     * Get all user preferences
     * @param request HTTP request to extract user ID from JWT
     * @return All user preferences
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all preferences", description = "Get all user preferences")
    public ResponseEntity<UserPreferencesDTO> getAllPreferences(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        log.debug("Getting all preferences for user: {}", userId);

        UserPreferences preferences = preferencesService.getUserPreferences(userId);
        UserPreferencesDTO dto = convertToDTO(preferences);

        return ResponseEntity.ok(dto);
    }

    /**
     * Update user preferences
     * @param request HTTP request to extract user ID from JWT
     * @param dto Preferences DTO
     * @return Updated preferences
     */
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update preferences", description = "Update user preferences")
    public ResponseEntity<UserPreferencesDTO> updatePreferences(
            HttpServletRequest request,
            @Valid @RequestBody UserPreferencesDTO dto) {
        Long userId = getUserIdFromRequest(request);
        log.info("Updating preferences for user: {}", userId);

        UserPreferences preferences = preferencesService.updatePreferences(userId, dto);
        UserPreferencesDTO responseDto = convertToDTO(preferences);

        return ResponseEntity.ok(responseDto);
    }

    /**
     * Extract user ID from JWT token in request
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = jwtTokenProvider.resolveToken(request);
        return jwtTokenProvider.getUserIdFromToken(token);
    }

    /**
     * Convert entity to DTO
     */
    private UserPreferencesDTO convertToDTO(UserPreferences preferences) {
        UserPreferencesDTO dto = new UserPreferencesDTO();
        dto.setTheme(preferences.getTheme());
        dto.setLanguage(preferences.getLanguage());
        dto.setNotificationsEnabled(preferences.isNotificationsEnabled());
        dto.setEmailNotifications(preferences.isEmailNotifications());
        return dto;
    }
}
