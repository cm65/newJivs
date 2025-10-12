package com.jivs.platform.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user preferences.
 * Part of Sprint 2 - Workflow 6: Dark Mode Implementation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesDTO {

    @NotNull(message = "Theme cannot be null")
    @Pattern(regexp = "^(light|dark|auto)$", message = "Theme must be 'light', 'dark', or 'auto'")
    private String theme;

    @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "Language must be valid ISO 639-1 code")
    private String language;

    private Boolean notificationsEnabled;

    private Boolean emailNotifications;
}
