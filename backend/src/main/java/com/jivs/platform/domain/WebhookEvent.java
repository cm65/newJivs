package com.jivs.platform.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

/**
 * Webhook event execution history
 */
@Entity
@Table(name = "webhook_events_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_id", nullable = false)
    private Webhook webhook;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload; // JSON payload sent

    @Column(name = "status")
    private String status; // SUCCESS, FAILED, PENDING, RETRYING

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "attempt_count")
    private int attemptCount = 1;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "completed_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
    }
}
