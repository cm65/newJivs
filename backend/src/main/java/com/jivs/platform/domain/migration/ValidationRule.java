package com.jivs.platform.domain.migration;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Validation rule entity for migration validation
 */
@Entity
@Table(name = "validation_rules", indexes = {
        @Index(name = "idx_validation_rules_rule_type", columnList = "rule_type"),
        @Index(name = "idx_validation_rules_active", columnList = "active"),
        @Index(name = "idx_validation_rules_source_system", columnList = "source_system")
})
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType;

    @Column(name = "source_system", length = 100)
    private String sourceSystem;

    @Column(name = "target_system", length = 100)
    private String targetSystem;

    @Column(name = "rule_definition", columnDefinition = "TEXT")
    private String ruleDefinition;

    @Column(name = "completeness_threshold")
    private Double completenessThreshold;

    @Column(name = "uniqueness_threshold")
    private Double uniquenessThreshold;

    @Column(name = "accuracy_threshold")
    private Double accuracyThreshold;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean critical = false;

    @Column(name = "execution_order")
    private Integer executionOrder = 0;

    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(name = "updated_date", nullable = false)
    private LocalDateTime updatedDate;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
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

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getTargetSystem() {
        return targetSystem;
    }

    public void setTargetSystem(String targetSystem) {
        this.targetSystem = targetSystem;
    }

    public String getRuleDefinition() {
        return ruleDefinition;
    }

    public void setRuleDefinition(String ruleDefinition) {
        this.ruleDefinition = ruleDefinition;
    }

    public Double getCompletenessThreshold() {
        return completenessThreshold;
    }

    public void setCompletenessThreshold(Double completenessThreshold) {
        this.completenessThreshold = completenessThreshold;
    }

    public Double getUniquenessThreshold() {
        return uniquenessThreshold;
    }

    public void setUniquenessThreshold(Double uniquenessThreshold) {
        this.uniquenessThreshold = uniquenessThreshold;
    }

    public Double getAccuracyThreshold() {
        return accuracyThreshold;
    }

    public void setAccuracyThreshold(Double accuracyThreshold) {
        this.accuracyThreshold = accuracyThreshold;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isCritical() {
        return critical;
    }

    public void setCritical(boolean critical) {
        this.critical = critical;
    }

    public Integer getExecutionOrder() {
        return executionOrder;
    }

    public void setExecutionOrder(Integer executionOrder) {
        this.executionOrder = executionOrder;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
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
