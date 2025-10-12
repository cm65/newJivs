package com.jivs.platform.domain.businessobject;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing a lifecycle event for a business object
 */
@Entity
@Table(name = "lifecycle_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LifecycleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lifecycle_id", nullable = false)
    private BusinessObjectLifecycle lifecycle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LifecycleState fromState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LifecycleState toState;

    @Column(nullable = false)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime eventTime;

    private String triggeredBy;

    @Column(columnDefinition = "TEXT")
    private String metadata;
}
