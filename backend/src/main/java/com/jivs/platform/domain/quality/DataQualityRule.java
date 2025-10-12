package com.jivs.platform.domain.quality;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Entity representing a data quality rule
 */
@Entity
@Table(name = "data_quality_rules")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DataQualityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(nullable = false)
    private String fieldPath;

    @Column(nullable = false)
    private boolean required = false;

    private String formatPattern;

    @ElementCollection
    @CollectionTable(name = "rule_reference_data", joinColumns = @JoinColumn(name = "rule_id"))
    @Column(name = "reference_value")
    private Set<String> referenceData;

    @Column(columnDefinition = "TEXT")
    private String consistencyExpression;

    @ElementCollection
    @CollectionTable(name = "rule_related_fields", joinColumns = @JoinColumn(name = "rule_id"))
    @Column(name = "field_name")
    private List<String> relatedFields;

    private String expectedDataType;

    private String minValue;

    private String maxValue;

    @ElementCollection
    @CollectionTable(name = "rule_allowed_values", joinColumns = @JoinColumn(name = "rule_id"))
    @Column(name = "allowed_value")
    private Set<String> allowedValues;

    private Duration timelinessThreshold;

    private String referenceTable;

    private String referenceColumn;

    @Column(columnDefinition = "TEXT")
    private String businessRuleExpression;

    private String scope;

    @Column(nullable = false)
    private String datasetType;

    @Column(nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getFieldPath() {
        return fieldPath;
    }

    public void setFieldPath(String fieldPath) {
        this.fieldPath = fieldPath;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getFormatPattern() {
        return formatPattern;
    }

    public void setFormatPattern(String formatPattern) {
        this.formatPattern = formatPattern;
    }

    public Set<String> getReferenceData() {
        return referenceData;
    }

    public void setReferenceData(Set<String> referenceData) {
        this.referenceData = referenceData;
    }

    public String getConsistencyExpression() {
        return consistencyExpression;
    }

    public void setConsistencyExpression(String consistencyExpression) {
        this.consistencyExpression = consistencyExpression;
    }

    public List<String> getRelatedFields() {
        return relatedFields;
    }

    public void setRelatedFields(List<String> relatedFields) {
        this.relatedFields = relatedFields;
    }

    public String getExpectedDataType() {
        return expectedDataType;
    }

    public void setExpectedDataType(String expectedDataType) {
        this.expectedDataType = expectedDataType;
    }

    public String getMinValue() {
        return minValue;
    }

    public void setMinValue(String minValue) {
        this.minValue = minValue;
    }

    public String getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(String maxValue) {
        this.maxValue = maxValue;
    }

    public Set<String> getAllowedValues() {
        return allowedValues;
    }

    public void setAllowedValues(Set<String> allowedValues) {
        this.allowedValues = allowedValues;
    }

    public Duration getTimelinessThreshold() {
        return timelinessThreshold;
    }

    public void setTimelinessThreshold(Duration timelinessThreshold) {
        this.timelinessThreshold = timelinessThreshold;
    }

    public String getReferenceTable() {
        return referenceTable;
    }

    public void setReferenceTable(String referenceTable) {
        this.referenceTable = referenceTable;
    }

    public String getReferenceColumn() {
        return referenceColumn;
    }

    public void setReferenceColumn(String referenceColumn) {
        this.referenceColumn = referenceColumn;
    }

    public String getBusinessRuleExpression() {
        return businessRuleExpression;
    }

    public void setBusinessRuleExpression(String businessRuleExpression) {
        this.businessRuleExpression = businessRuleExpression;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(String datasetType) {
        this.datasetType = datasetType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
