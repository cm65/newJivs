package com.jivs.platform.domain.quality;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing a data profile with statistics and patterns
 */
@Entity
@Table(name = "data_profiles")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DataProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long datasetId;

    @Column(nullable = false)
    private String datasetType;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime profiledAt;

    private Long totalRecords;

    private Long totalFields;

    @ElementCollection
    @CollectionTable(name = "profile_field_statistics", joinColumns = @JoinColumn(name = "profile_id"))
    @MapKeyColumn(name = "field_name")
    @Column(name = "statistics", columnDefinition = "TEXT")
    private Map<String, String> fieldStatistics = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "profile_data_types", joinColumns = @JoinColumn(name = "profile_id"))
    @MapKeyColumn(name = "field_name")
    @Column(name = "data_type")
    private Map<String, String> dataTypes = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "profile_null_counts", joinColumns = @JoinColumn(name = "profile_id"))
    @MapKeyColumn(name = "field_name")
    @Column(name = "null_count")
    private Map<String, Long> nullCounts = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "profile_distinct_counts", joinColumns = @JoinColumn(name = "profile_id"))
    @MapKeyColumn(name = "field_name")
    @Column(name = "distinct_count")
    private Map<String, Long> distinctCounts = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "profile_patterns", joinColumns = @JoinColumn(name = "profile_id"))
    @MapKeyColumn(name = "field_name")
    @Column(name = "pattern")
    private Map<String, String> patterns = new HashMap<>();

    private String profiledBy;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(Long datasetId) {
        this.datasetId = datasetId;
    }

    public String getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(String datasetType) {
        this.datasetType = datasetType;
    }

    public LocalDateTime getProfiledAt() {
        return profiledAt;
    }

    public void setProfiledAt(LocalDateTime profiledAt) {
        this.profiledAt = profiledAt;
    }

    public Long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Long totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Long getTotalFields() {
        return totalFields;
    }

    public void setTotalFields(Long totalFields) {
        this.totalFields = totalFields;
    }

    public Map<String, String> getFieldStatistics() {
        return fieldStatistics;
    }

    public void setFieldStatistics(Map<String, String> fieldStatistics) {
        this.fieldStatistics = fieldStatistics;
    }

    public Map<String, String> getDataTypes() {
        return dataTypes;
    }

    public void setDataTypes(Map<String, String> dataTypes) {
        this.dataTypes = dataTypes;
    }

    public Map<String, Long> getNullCounts() {
        return nullCounts;
    }

    public void setNullCounts(Map<String, Long> nullCounts) {
        this.nullCounts = nullCounts;
    }

    public Map<String, Long> getDistinctCounts() {
        return distinctCounts;
    }

    public void setDistinctCounts(Map<String, Long> distinctCounts) {
        this.distinctCounts = distinctCounts;
    }

    public Map<String, String> getPatterns() {
        return patterns;
    }

    public void setPatterns(Map<String, String> patterns) {
        this.patterns = patterns;
    }

    public String getProfiledBy() {
        return profiledBy;
    }

    public void setProfiledBy(String profiledBy) {
        this.profiledBy = profiledBy;
    }
}
