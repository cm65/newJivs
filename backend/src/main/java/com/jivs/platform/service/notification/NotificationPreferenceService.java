package com.jivs.platform.service.notification;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing user notification preferences
 */
@Service
public class NotificationPreferenceService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NotificationPreferenceService.class);

    /**
     * Get user's notification preferences
     */
    public NotificationPreferences getUserPreferences(String userId) {
        log.debug("Getting notification preferences for user: {}", userId);

        // TODO: Fetch from database
        // For now, return default preferences
        NotificationPreferences preferences = new NotificationPreferences();
        preferences.setUserId(userId);
        preferences.setEmailEnabled(true);
        preferences.setInAppEnabled(true);
        preferences.setSmsEnabled(false);
        preferences.setWebhookEnabled(false);

        // Default preferences by type
        Map<NotificationType, Map<NotificationChannel, Boolean>> typePreferences = new HashMap<>();

        // Extraction notifications - email and in-app
        Map<NotificationChannel, Boolean> extractionPrefs = new HashMap<>();
        extractionPrefs.put(NotificationChannel.EMAIL, true);
        extractionPrefs.put(NotificationChannel.IN_APP, true);
        extractionPrefs.put(NotificationChannel.SMS, false);
        typePreferences.put(NotificationType.EXTRACTION_COMPLETE, extractionPrefs);
        typePreferences.put(NotificationType.EXTRACTION_FAILED, extractionPrefs);

        // Migration notifications - email and in-app
        Map<NotificationChannel, Boolean> migrationPrefs = new HashMap<>();
        migrationPrefs.put(NotificationChannel.EMAIL, true);
        migrationPrefs.put(NotificationChannel.IN_APP, true);
        migrationPrefs.put(NotificationChannel.SMS, false);
        typePreferences.put(NotificationType.MIGRATION_COMPLETE, migrationPrefs);
        typePreferences.put(NotificationType.MIGRATION_FAILED, migrationPrefs);

        // System alerts - all channels
        Map<NotificationChannel, Boolean> alertPrefs = new HashMap<>();
        alertPrefs.put(NotificationChannel.EMAIL, true);
        alertPrefs.put(NotificationChannel.IN_APP, true);
        alertPrefs.put(NotificationChannel.SMS, true);
        typePreferences.put(NotificationType.SYSTEM_ALERT, alertPrefs);
        typePreferences.put(NotificationType.DATA_QUALITY_ALERT, alertPrefs);

        // Compliance - email and in-app
        Map<NotificationChannel, Boolean> compliancePrefs = new HashMap<>();
        compliancePrefs.put(NotificationChannel.EMAIL, true);
        compliancePrefs.put(NotificationChannel.IN_APP, true);
        compliancePrefs.put(NotificationChannel.SMS, false);
        typePreferences.put(NotificationType.COMPLIANCE_REQUEST, compliancePrefs);

        preferences.setTypePreferences(typePreferences);

        return preferences;
    }

    /**
     * Update user's notification preferences
     */
    public void updatePreferences(String userId, NotificationPreferences preferences) {
        log.info("Updating notification preferences for user: {}", userId);

        // TODO: Save to database
    }

    /**
     * Enable/disable specific notification channel
     */
    public void setChannelEnabled(String userId, NotificationChannel channel, boolean enabled) {
        log.info("Setting channel {} to {} for user: {}", channel, enabled, userId);

        // TODO: Update database
    }

    /**
     * Enable/disable notifications for specific type
     */
    public void setTypeEnabled(String userId, NotificationType type, boolean enabled) {
        log.info("Setting notification type {} to {} for user: {}", type, enabled, userId);

        // TODO: Update database
    }
}

/**
 * User notification preferences
 */
class NotificationPreferences {
    private String userId;
    private boolean emailEnabled;
    private boolean smsEnabled;
    private boolean inAppEnabled;
    private boolean webhookEnabled;
    private Map<NotificationType, Map<NotificationChannel, Boolean>> typePreferences;

    public boolean isEmailEnabled(NotificationType type) {
        if (typePreferences != null && typePreferences.containsKey(type)) {
            return typePreferences.get(type).getOrDefault(NotificationChannel.EMAIL, emailEnabled);
        }
        return emailEnabled;
    }

    public boolean isSmsEnabled(NotificationType type) {
        if (typePreferences != null && typePreferences.containsKey(type)) {
            return typePreferences.get(type).getOrDefault(NotificationChannel.SMS, smsEnabled);
        }
        return smsEnabled;
    }

    public boolean isInAppEnabled(NotificationType type) {
        if (typePreferences != null && typePreferences.containsKey(type)) {
            return typePreferences.get(type).getOrDefault(NotificationChannel.IN_APP, inAppEnabled);
        }
        return inAppEnabled;
    }

    public boolean isWebhookEnabled(NotificationType type) {
        if (typePreferences != null && typePreferences.containsKey(type)) {
            return typePreferences.get(type).getOrDefault(NotificationChannel.WEBHOOK, webhookEnabled);
        }
        return webhookEnabled;
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public boolean isEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(boolean emailEnabled) { this.emailEnabled = emailEnabled; }
    public boolean isSmsEnabled() { return smsEnabled; }
    public void setSmsEnabled(boolean smsEnabled) { this.smsEnabled = smsEnabled; }
    public boolean isInAppEnabled() { return inAppEnabled; }
    public void setInAppEnabled(boolean inAppEnabled) { this.inAppEnabled = inAppEnabled; }
    public boolean isWebhookEnabled() { return webhookEnabled; }
    public void setWebhookEnabled(boolean webhookEnabled) { this.webhookEnabled = webhookEnabled; }
    public Map<NotificationType, Map<NotificationChannel, Boolean>> getTypePreferences() {
        return typePreferences;
    }
    public void setTypePreferences(Map<NotificationType, Map<NotificationChannel, Boolean>> typePreferences) {
        this.typePreferences = typePreferences;
    }
}
