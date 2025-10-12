package com.jivs.platform.service.user;

import com.jivs.platform.domain.UserPreferences;
import com.jivs.platform.dto.UserPreferencesDTO;
import com.jivs.platform.repository.UserPreferencesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing user preferences including theme, language, and notification settings.
 * Part of Sprint 2 - Workflow 6: Dark Mode Implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserPreferencesService {

    private final UserPreferencesRepository repository;

    /**
     * Get user preferences, creating default if not exists
     * Note: Not readOnly because it may create default preferences
     *
     * @param userId User ID
     * @return User preferences
     */
    @Transactional
    public UserPreferences getUserPreferences(Long userId) {
        log.debug("Getting preferences for user: {}", userId);
        return repository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
    }

    /**
     * Update theme preference
     *
     * @param userId User ID
     * @param theme Theme value (light, dark, auto)
     * @return Updated preferences
     */
    @Transactional
    public UserPreferences updateTheme(Long userId, String theme) {
        log.info("Updating theme for user {} to: {}", userId, theme);

        UserPreferences preferences = repository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));

        preferences.setTheme(theme);
        return repository.save(preferences);
    }

    /**
     * Update all user preferences
     *
     * @param userId User ID
     * @param dto Preferences DTO
     * @return Updated preferences
     */
    @Transactional
    public UserPreferences updatePreferences(Long userId, UserPreferencesDTO dto) {
        log.info("Updating all preferences for user: {}", userId);

        UserPreferences preferences = repository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));

        if (dto.getTheme() != null) {
            preferences.setTheme(dto.getTheme());
        }
        if (dto.getLanguage() != null) {
            preferences.setLanguage(dto.getLanguage());
        }
        if (dto.getNotificationsEnabled() != null) {
            preferences.setNotificationsEnabled(dto.getNotificationsEnabled());
        }
        if (dto.getEmailNotifications() != null) {
            preferences.setEmailNotifications(dto.getEmailNotifications());
        }

        return repository.save(preferences);
    }

    /**
     * Delete user preferences
     *
     * @param userId User ID
     */
    @Transactional
    public void deleteUserPreferences(Long userId) {
        log.info("Deleting preferences for user: {}", userId);
        repository.deleteByUserId(userId);
    }

    /**
     * Create default preferences for a user
     *
     * @param userId User ID
     * @return Default preferences
     */
    @Transactional
    public UserPreferences createDefaultPreferences(Long userId) {
        log.debug("Creating default preferences for user: {}", userId);

        UserPreferences preferences = UserPreferences.builder()
                .userId(userId)
                .theme("light")
                .language("en")
                .notificationsEnabled(true)
                .emailNotifications(true)
                .build();

        return repository.save(preferences);
    }

    /**
     * Check if user has preferences
     *
     * @param userId User ID
     * @return true if preferences exist
     */
    @Transactional(readOnly = true)
    public boolean hasPreferences(Long userId) {
        return repository.existsByUserId(userId);
    }
}
