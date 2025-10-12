package com.jivs.platform.domain.quality;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing a detected data anomaly
 */
@Entity
@Table(name = "data_anomalies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DataAnomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long datasetId;

    @Column(nullable = false)
    private String datasetType;

    @Column(nullable = false)
    private String fieldName;

    @Column(nullable = false)
    private String anomalyType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(columnDefinition = "TEXT")
    private String detectedValue;

    private Double confidence;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    private String detectedBy;

    private boolean resolved = false;

    private LocalDateTime resolvedAt;

    private String resolvedBy;

    @Column(columnDefinition = "TEXT")
    private String resolutionNotes;
}
