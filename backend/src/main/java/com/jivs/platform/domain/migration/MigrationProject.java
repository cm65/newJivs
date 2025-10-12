package com.jivs.platform.domain.migration;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Migration Project entity
 */
@Entity
@Table(name = "migration_projects", indexes = {
        @Index(name = "idx_migration_projects_code", columnList = "project_code"),
        @Index(name = "idx_migration_projects_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_code", nullable = false, unique = true, length = 50)
    private String projectCode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "project_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ProjectType projectType;

    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem;

    @Column(name = "target_system", nullable = false, length = 100)
    private String targetSystem;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ProjectStatus status;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "planned_cutover_date")
    private LocalDate plannedCutoverDate;

    @Column(name = "actual_cutover_date")
    private LocalDate actualCutoverDate;

    @Column(name = "estimated_records")
    private Long estimatedRecords;

    @Column(name = "estimated_size_gb", precision = 10, scale = 2)
    private BigDecimal estimatedSizeGb;

    @ElementCollection
    @CollectionTable(name = "migration_project_metadata",
                     joinColumns = @JoinColumn(name = "migration_project_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    @Builder.Default
    private Map<String, String> projectMetadata = new HashMap<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    public enum ProjectType {
        DATA_MIGRATION, APP_RETIREMENT, SYSTEM_CONSOLIDATION
    }

    public enum ProjectStatus {
        PLANNING, IN_PROGRESS, ON_HOLD, COMPLETED, CANCELLED
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}