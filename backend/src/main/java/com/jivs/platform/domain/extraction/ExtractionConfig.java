package com.jivs.platform.domain.extraction;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an extraction configuration
 * Maps to extraction_configs table
 */
@Entity
@Table(name = "extraction_configs")
@Data
public class ExtractionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_source_id", nullable = false)
    private DataSource dataSource;

    @Column(name = "extraction_type", nullable = false, length = 50)
    private String extractionType; // FULL, INCREMENTAL, DELTA

    @Column(name = "extraction_query", columnDefinition = "TEXT")
    private String extractionQuery;

    @Column(name = "where_clause", columnDefinition = "TEXT")
    private String whereClause;

    @Column(name = "incremental_field", length = 100)
    private String incrementalField;

    @Column(name = "last_extracted_value", length = 255)
    private String lastExtractedValue;

    @Column(name = "batch_size", nullable = false)
    private Integer batchSize = 1000;

    @Column(name = "parallel_threads", nullable = false)
    private Integer parallelThreads = 1;

    @Column(name = "timeout_minutes", nullable = false)
    private Integer timeoutMinutes = 60;

    @Column(name = "retry_attempts", nullable = false)
    private Integer retryAttempts = 3;

    @Column(name = "schedule_expression", length = 100)
    private String scheduleExpression;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;
}
