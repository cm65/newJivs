package com.jivs.platform.service.notification;

import java.util.List;
import java.util.Map;

/**
 * Notification request
 */
public class NotificationRequest {
    private String userId;
    private NotificationType type;
    private String subject;
    private String message;
    private Map<String, Object> data;
    private List<NotificationChannel> channels;
    private NotificationPriority priority;
    private String recipientEmail;
    private String recipientPhone;
    private String fromEmail;
    private String webhookUrl;
    private List<NotificationAttachment> attachments;

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    public List<NotificationChannel> getChannels() { return channels; }
    public void setChannels(List<NotificationChannel> channels) { this.channels = channels; }
    public NotificationPriority getPriority() { return priority; }
    public void setPriority(NotificationPriority priority) { this.priority = priority; }
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }
    public String getFromEmail() { return fromEmail; }
    public void setFromEmail(String fromEmail) { this.fromEmail = fromEmail; }
    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
    public List<NotificationAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<NotificationAttachment> attachments) {
        this.attachments = attachments;
    }
}
