package com.jivs.platform.domain.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Entity representing an audit log entry
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_user_id", columnList = "userId"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_entity_type", columnList = "entityType"),
    @Index(name = "idx_audit_entity_id", columnList = "entityId"),
    @Index(name = "idx_audit_severity", columnList = "severity"),
    @Index(name = "idx_audit_event_type", columnList = "eventType"),
    @Index(name = "idx_audit_session", columnList = "sessionId")
})
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuditLog.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditSeverity severity = AuditSeverity.INFO;

    // User information
    private Long userId;

    @Column(length = 100)
    private String username;

    // Event information
    @Column(nullable = false, length = 100)
    private String eventType;

    // Entity information (for tracking what was affected)
    @Column(length = 100)
    private String entityType;

    @Column(length = 100)
    private String entityId;

    // Data changes
    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    // Request information
    @Column(nullable = false, length = 50)
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String userAgent;

    @Column(length = 100)
    private String sessionId;

    @Column(length = 10)
    private String requestMethod;

    @Column(length = 500)
    private String requestUrl;

    private Integer statusCode;

    // Operation details
    @Column(nullable = false)
    private boolean success = true;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    // Performance tracking
    private Long duration; // in milliseconds

    // Application context
    @Column(length = 50)
    private String applicationVersion;

    @Column(length = 50)
    private String environment;

    @Column(length = 100)
    private String correlationId;

    // Metadata and tags (stored as JSON)
    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    @Column(columnDefinition = "TEXT")
    private String tagsJson;

    // Transient fields for easy access
    @Transient
    private Map<String, Object> metadata;

    @Transient
    private List<String> tags;

    // Custom getters and setters for metadata
    public Map<String, Object> getMetadata() {
        if (metadata == null && metadataJson != null && !metadataJson.isEmpty()) {
            try {
                metadata = objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                log.error("Error parsing metadata JSON", e);
                metadata = new HashMap<>();
            }
        }
        return metadata != null ? metadata : new HashMap<>();
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        if (metadata != null && !metadata.isEmpty()) {
            try {
                this.metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (JsonProcessingException e) {
                log.error("Error serializing metadata to JSON", e);
                this.metadataJson = "{}";
            }
        } else {
            this.metadataJson = null;
        }
    }

    // Custom getters and setters for tags
    public List<String> getTags() {
        if (tags == null && tagsJson != null && !tagsJson.isEmpty()) {
            try {
                tags = objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e) {
                log.error("Error parsing tags JSON", e);
                tags = new ArrayList<>();
            }
        }
        return tags != null ? tags : new ArrayList<>();
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
        if (tags != null && !tags.isEmpty()) {
            try {
                this.tagsJson = objectMapper.writeValueAsString(tags);
            } catch (JsonProcessingException e) {
                log.error("Error serializing tags to JSON", e);
                this.tagsJson = "[]";
            }
        } else {
            this.tagsJson = null;
        }
    }

    // Convenience methods for backward compatibility
    @Deprecated
    public String getResourceType() {
        return entityType;
    }

    @Deprecated
    public void setResourceType(String resourceType) {
        this.entityType = resourceType;
    }

    @Deprecated
    public String getResourceId() {
        return entityId;
    }

    @Deprecated
    public void setResourceId(String resourceId) {
        this.entityId = resourceId;
    }

    // Additional convenience methods
    public String getUserName() {
        return username;
    }

    public void setUserName(String userName) {
        this.username = userName;
    }

    // Getters and Setters for all fields
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public AuditSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AuditSeverity severity) {
        this.severity = severity;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public String getTagsJson() {
        return tagsJson;
    }

    public void setTagsJson(String tagsJson) {
        this.tagsJson = tagsJson;
    }
}
