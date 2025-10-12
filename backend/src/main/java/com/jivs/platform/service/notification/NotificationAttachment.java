package com.jivs.platform.service.notification;

/**
 * Notification attachment
 */
public class NotificationAttachment {
    private String name;
    private byte[] content;
    private String contentType;

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public byte[] getContent() { return content; }
    public void setContent(byte[] content) { this.content = content; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
}
