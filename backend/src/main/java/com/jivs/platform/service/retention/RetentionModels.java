package com.jivs.platform.service.retention;

import com.jivs.platform.domain.retention.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Retention policy request
 */
class RetentionPolicyRequest {
    private String name;
    private String description;
    private String entityType;
    private int retentionPeriod;
    private RetentionUnit retentionUnit;
    private RetentionAction action;
    private String scope;
    private Map<String, Object> conditions;
    private int priority = 0;
    private Long userId;

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public int getRetentionPeriod() { return retentionPeriod; }
    public void setRetentionPeriod(int retentionPeriod) { this.retentionPeriod = retentionPeriod; }
    public RetentionUnit getRetentionUnit() { return retentionUnit; }
    public void setRetentionUnit(RetentionUnit retentionUnit) { this.retentionUnit = retentionUnit; }
    public RetentionAction getAction() { return action; }
    public void setAction(RetentionAction action) { this.action = action; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public Map<String, Object> getConditions() { return conditions; }
    public void setConditions(Map<String, Object> conditions) { this.conditions = conditions; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}

/**
 * Retention policy update request
 */
class RetentionPolicyUpdateRequest {
    private String name;
    private String description;
    private Integer retentionPeriod;
    private RetentionUnit retentionUnit;
    private RetentionAction action;
    private Map<String, Object> conditions;
    private Long userId;

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getRetentionPeriod() { return retentionPeriod; }
    public void setRetentionPeriod(Integer retentionPeriod) {
        this.retentionPeriod = retentionPeriod;
    }
    public RetentionUnit getRetentionUnit() { return retentionUnit; }
    public void setRetentionUnit(RetentionUnit retentionUnit) { this.retentionUnit = retentionUnit; }
    public RetentionAction getAction() { return action; }
    public void setAction(RetentionAction action) { this.action = action; }
    public Map<String, Object> getConditions() { return conditions; }
    public void setConditions(Map<String, Object> conditions) { this.conditions = conditions; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}

/**
 * Retention execution result
 */
class RetentionExecutionResult {
    private Long policyId;
    private String policyName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long duration;
    private int totalRecords;
    private int processed;
    private int failed;
    private Map<Object, String> errors = new HashMap<>();

    public void incrementProcessed() { processed++; }
    public void incrementFailed() { failed++; }
    public void addError(Object recordId, String error) {
        errors.put(recordId, error);
    }

    // Getters and setters
    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }
    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
    public int getProcessed() { return processed; }
    public void setProcessed(int processed) { this.processed = processed; }
    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }
    public Map<Object, String> getErrors() { return errors; }
    public void setErrors(Map<Object, String> errors) { this.errors = errors; }
}

/**
 * Retention status information
 */
class RetentionStatusInfo {
    private String entityType;
    private Long recordId;
    private boolean hasRetentionPolicy;
    private List<RetentionPolicy> policies;
    private RetentionPolicy primaryPolicy;
    private LocalDateTime expiryDate;
    private long daysRemaining;
    private boolean actionDue;
    private List<RetentionRecord> retentionHistory;

    // Getters and setters
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public boolean isHasRetentionPolicy() { return hasRetentionPolicy; }
    public void setHasRetentionPolicy(boolean hasRetentionPolicy) {
        this.hasRetentionPolicy = hasRetentionPolicy;
    }
    public List<RetentionPolicy> getPolicies() { return policies; }
    public void setPolicies(List<RetentionPolicy> policies) { this.policies = policies; }
    public RetentionPolicy getPrimaryPolicy() { return primaryPolicy; }
    public void setPrimaryPolicy(RetentionPolicy primaryPolicy) {
        this.primaryPolicy = primaryPolicy;
    }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    public long getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(long daysRemaining) { this.daysRemaining = daysRemaining; }
    public boolean isActionDue() { return actionDue; }
    public void setActionDue(boolean actionDue) { this.actionDue = actionDue; }
    public List<RetentionRecord> getRetentionHistory() { return retentionHistory; }
    public void setRetentionHistory(List<RetentionRecord> retentionHistory) {
        this.retentionHistory = retentionHistory;
    }
}

/**
 * Retention hold request
 */
class RetentionHoldRequest {
    private String reason;
    private String holdType;
    private LocalDateTime expiryDate;
    private Long userId;

    // Getters and setters
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getHoldType() { return holdType; }
    public void setHoldType(String holdType) { this.holdType = holdType; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}

/**
 * Retention hold model
 */
class RetentionHold {
    private Long id;
    private String entityType;
    private Long recordId;
    private String reason;
    private String holdType;
    private LocalDateTime appliedDate;
    private Long appliedBy;
    private LocalDateTime expiryDate;
    private boolean active;
    private LocalDateTime releasedDate;
    private Long releasedBy;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getHoldType() { return holdType; }
    public void setHoldType(String holdType) { this.holdType = holdType; }
    public LocalDateTime getAppliedDate() { return appliedDate; }
    public void setAppliedDate(LocalDateTime appliedDate) { this.appliedDate = appliedDate; }
    public Long getAppliedBy() { return appliedBy; }
    public void setAppliedBy(Long appliedBy) { this.appliedBy = appliedBy; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getReleasedDate() { return releasedDate; }
    public void setReleasedDate(LocalDateTime releasedDate) { this.releasedDate = releasedDate; }
    public Long getReleasedBy() { return releasedBy; }
    public void setReleasedBy(Long releasedBy) { this.releasedBy = releasedBy; }
}

/**
 * Retention statistics
 */
class RetentionStatistics {
    private long totalPolicies;
    private long activePolicies;
    private long totalRetentionActions;
    private Map<RetentionAction, Long> actionsByType;
    private Map<RetentionStatus, Long> actionsByStatus;
    private List<RetentionRecord> recentActions;
    private long spaceSavedBytes;

    // Getters and setters
    public long getTotalPolicies() { return totalPolicies; }
    public void setTotalPolicies(long totalPolicies) { this.totalPolicies = totalPolicies; }
    public long getActivePolicies() { return activePolicies; }
    public void setActivePolicies(long activePolicies) { this.activePolicies = activePolicies; }
    public long getTotalRetentionActions() { return totalRetentionActions; }
    public void setTotalRetentionActions(long totalRetentionActions) {
        this.totalRetentionActions = totalRetentionActions;
    }
    public Map<RetentionAction, Long> getActionsByType() { return actionsByType; }
    public void setActionsByType(Map<RetentionAction, Long> actionsByType) {
        this.actionsByType = actionsByType;
    }
    public Map<RetentionStatus, Long> getActionsByStatus() { return actionsByStatus; }
    public void setActionsByStatus(Map<RetentionStatus, Long> actionsByStatus) {
        this.actionsByStatus = actionsByStatus;
    }
    public List<RetentionRecord> getRecentActions() { return recentActions; }
    public void setRecentActions(List<RetentionRecord> recentActions) {
        this.recentActions = recentActions;
    }
    public long getSpaceSavedBytes() { return spaceSavedBytes; }
    public void setSpaceSavedBytes(long spaceSavedBytes) { this.spaceSavedBytes = spaceSavedBytes; }
}

/**
 * Retention policy preview
 */
class RetentionPolicyPreview {
    private RetentionPolicy policy;
    private int affectedRecords;
    private long totalSizeBytes;
    private Map<String, Long> recordsByCategory;
    private Map<String, Long> recordsByAge;

    // Getters and setters
    public RetentionPolicy getPolicy() { return policy; }
    public void setPolicy(RetentionPolicy policy) { this.policy = policy; }
    public int getAffectedRecords() { return affectedRecords; }
    public void setAffectedRecords(int affectedRecords) { this.affectedRecords = affectedRecords; }
    public long getTotalSizeBytes() { return totalSizeBytes; }
    public void setTotalSizeBytes(long totalSizeBytes) { this.totalSizeBytes = totalSizeBytes; }
    public Map<String, Long> getRecordsByCategory() { return recordsByCategory; }
    public void setRecordsByCategory(Map<String, Long> recordsByCategory) {
        this.recordsByCategory = recordsByCategory;
    }
    public Map<String, Long> getRecordsByAge() { return recordsByAge; }
    public void setRecordsByAge(Map<String, Long> recordsByAge) {
        this.recordsByAge = recordsByAge;
    }
}

/**
 * Bulk retention request
 */
class BulkRetentionRequest {
    private Long policyId;
    private String entityType;
    private List<Long> recordIds;

    // Getters and setters
    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public List<Long> getRecordIds() { return recordIds; }
    public void setRecordIds(List<Long> recordIds) { this.recordIds = recordIds; }
}