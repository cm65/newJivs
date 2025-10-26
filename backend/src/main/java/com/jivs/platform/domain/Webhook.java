package com.jivs.platform.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * Webhook entity for event notifications
 */
@Entity
@Table(name = "webhooks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Webhook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "secret", length = 64)
    private String secret; // For HMAC signature verification

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "webhook_events", joinColumns = @JoinColumn(name = "webhook_id"))
    @Column(name = "event_type")
    private List<String> eventTypes; // e.g., "DOCUMENT_UPLOADED", "DOCUMENT_ARCHIVED"

    @Column(name = "active")
    private boolean active = true;

    @Column(name = "retry_count")
    private int retryCount = 3;

    @Column(name = "timeout_seconds")
    private int timeoutSeconds = 30;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Column(name = "last_triggered_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastTriggeredAt;

    @Column(name = "success_count")
    private Long successCount = 0L;

    @Column(name = "failure_count")
    private Long failureCount = 0L;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}
