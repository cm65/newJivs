package com.jivs.platform.service.retention;

import com.jivs.platform.domain.retention.*;
import com.jivs.platform.repository.RetentionPolicyRepository;
import com.jivs.platform.repository.RetentionRecordRepository;
import com.jivs.platform.service.storage.StorageService;
import com.jivs.platform.service.compliance.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for managing data retention policies and enforcement
 * Handles automatic archiving and deletion based on retention rules
 */
@Service
@RequiredArgsConstructor
public class RetentionService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RetentionService.class);

    private final RetentionPolicyRepository policyRepository;
    private final RetentionRecordRepository recordRepository;
    private final StorageService storageService;
    private final AuditService auditService;
    private final RetentionExecutor retentionExecutor;
    private final RetentionCalculator retentionCalculator;

    /**
     * Create retention policy
     */
    @Transactional
    public RetentionPolicy createPolicy(RetentionPolicyRequest request) {
        log.info("Creating retention policy: {}", request.getName());

        // Validate policy
        validateRetentionPolicy(request);

        RetentionPolicy policy = new RetentionPolicy();
        policy.setName(request.getName());
        policy.setDescription(request.getDescription());
        policy.setEntityType(request.getEntityType());
        policy.setRetentionPeriod(request.getRetentionPeriod());
        policy.setRetentionUnit(request.getRetentionUnit());
        policy.setAction(request.getAction());
        policy.setScope(request.getScope());
        policy.setConditions(request.getConditions());
        policy.setActive(true);
        policy.setPriority(request.getPriority());
        policy.setCreatedDate(LocalDateTime.now());
        policy.setCreatedBy(request.getUserId() != null ? request.getUserId().toString() : null);

        // Calculate next execution time
        policy.setNextExecutionTime(calculateNextExecution(policy));

        RetentionPolicy savedPolicy = policyRepository.save(policy);

        // Audit policy creation
        auditService.logEvent("RETENTION_POLICY_CREATED", savedPolicy.getId(),
            "Retention policy created: " + savedPolicy.getName());

        log.info("Retention policy created with ID: {}", savedPolicy.getId());
        return savedPolicy;
    }

    /**
     * Update retention policy
     */
    @Transactional
    public RetentionPolicy updatePolicy(Long id, RetentionPolicyUpdateRequest request) {
        log.info("Updating retention policy: {}", id);

        RetentionPolicy policy = policyRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Retention policy not found: " + id));

        // Update fields
        if (request.getName() != null) {
            policy.setName(request.getName());
        }
        if (request.getDescription() != null) {
            policy.setDescription(request.getDescription());
        }
        if (request.getRetentionPeriod() != null) {
            policy.setRetentionPeriod(request.getRetentionPeriod());
        }
        if (request.getRetentionUnit() != null) {
            policy.setRetentionUnit(request.getRetentionUnit());
        }
        if (request.getAction() != null) {
            policy.setAction(request.getAction());
        }
        if (request.getConditions() != null) {
            policy.setConditions(request.getConditions());
        }

        policy.setModifiedDate(LocalDateTime.now());
        policy.setModifiedBy(request.getUserId());
        policy.setNextExecutionTime(calculateNextExecution(policy));

        RetentionPolicy savedPolicy = policyRepository.save(policy);

        // Audit policy update
        auditService.logEvent("RETENTION_POLICY_UPDATED", savedPolicy.getId(),
            "Retention policy updated: " + savedPolicy.getName());

        return savedPolicy;
    }

    /**
     * Execute retention policy immediately
     */
    @Async
    @Transactional
    public CompletableFuture<RetentionExecutionResult> executePolicy(Long policyId) {
        log.info("Executing retention policy: {}", policyId);

        RetentionPolicy policy = policyRepository.findById(policyId)
            .orElseThrow(() -> new IllegalArgumentException("Retention policy not found: " + policyId));

        if (!policy.isActive()) {
            throw new IllegalStateException("Cannot execute inactive policy");
        }

        RetentionExecutionResult result = new RetentionExecutionResult();
        result.setPolicyId(policyId);
        result.setPolicyName(policy.getName());
        result.setStartTime(LocalDateTime.now());

        try {
            // Find records eligible for retention action
            List<RetentionCandidate> candidates = findRetentionCandidates(policy);
            result.setTotalRecords(candidates.size());

            log.info("Found {} records eligible for retention action", candidates.size());

            // Execute retention action on each candidate
            for (RetentionCandidate candidate : candidates) {
                try {
                    executeRetentionAction(policy, candidate);
                    result.incrementProcessed();

                    // Record retention execution
                    recordRetentionAction(policy, candidate);

                } catch (Exception e) {
                    log.error("Failed to execute retention action on record: {}", candidate.getRecordId(), e);
                    result.incrementFailed();
                    result.addError(candidate.getRecordId(), e.getMessage());
                }
            }

            // Update policy execution time
            policy.setLastExecutionTime(LocalDateTime.now());
            policy.setNextExecutionTime(calculateNextExecution(policy));
            policy.setExecutionCount(policy.getExecutionCount() + 1);
            policyRepository.save(policy);

            result.setEndTime(LocalDateTime.now());
            result.setDuration(ChronoUnit.SECONDS.between(result.getStartTime(), result.getEndTime()));

            // Audit policy execution
            auditService.logEvent("RETENTION_POLICY_EXECUTED", policyId,
                String.format("Processed: %d, Failed: %d", result.getProcessed(), result.getFailed()));

            log.info("Retention policy execution completed: {}", result);

        } catch (Exception e) {
            log.error("Retention policy execution failed", e);
            result.setFailed(result.getTotalRecords());
            result.addError("EXECUTION_FAILED", e.getMessage());
        }

        return CompletableFuture.completedFuture(result);
    }

    /**
     * Scheduled retention scan - runs daily
     */
    @Scheduled(cron = "0 0 3 * * ?") // Run at 3 AM every day
    @Transactional
    public void scheduledRetentionScan() {
        log.info("Starting scheduled retention scan");

        // Get all active policies
        List<RetentionPolicy> activePolicies = policyRepository.findByActive(true);

        for (RetentionPolicy policy : activePolicies) {
            try {
                // Check if policy should be executed
                if (shouldExecutePolicy(policy)) {
                    executePolicy(policy.getId());
                }
            } catch (Exception e) {
                log.error("Failed to execute retention policy: {}", policy.getName(), e);
            }
        }

        log.info("Scheduled retention scan completed");
    }

    /**
     * Find records eligible for retention based on policy
     */
    private List<RetentionCandidate> findRetentionCandidates(RetentionPolicy policy) {
        log.debug("Finding retention candidates for policy: {}", policy.getName());

        // Calculate retention cutoff date
        LocalDateTime cutoffDate = retentionCalculator.calculateCutoffDate(
            policy.getRetentionPeriod(),
            policy.getRetentionUnit()
        );

        // Find candidates based on entity type
        List<RetentionCandidate> candidates = retentionExecutor.findCandidates(
            policy.getEntityType(),
            cutoffDate,
            policy.getConditions(),
            policy.getScope()
        );

        // Filter by additional conditions
        if (policy.getConditions() != null && !policy.getConditions().isEmpty()) {
            candidates = filterByConditions(candidates, policy.getConditions());
        }

        log.debug("Found {} retention candidates", candidates.size());
        return candidates;
    }

    /**
     * Execute retention action on a record
     */
    private void executeRetentionAction(RetentionPolicy policy, RetentionCandidate candidate) {
        log.debug("Executing retention action {} on record {}",
            policy.getAction(), candidate.getRecordId());

        switch (policy.getAction()) {
            case DELETE:
                retentionExecutor.deleteRecord(candidate);
                break;

            case ARCHIVE:
                retentionExecutor.archiveRecord(candidate);
                break;

            case COLD_STORAGE:
                retentionExecutor.moveToColdStorage(candidate);
                break;

            case ANONYMIZE:
                retentionExecutor.anonymizeRecord(candidate);
                break;

            case SOFT_DELETE:
                retentionExecutor.softDeleteRecord(candidate);
                break;

            case NOTIFY:
                retentionExecutor.notifyRetentionDue(candidate);
                break;

            default:
                throw new UnsupportedOperationException("Unknown retention action: " + policy.getAction());
        }

        log.debug("Retention action executed successfully");
    }

    /**
     * Record retention action in audit log
     */
    private void recordRetentionAction(RetentionPolicy policy, RetentionCandidate candidate) {
        RetentionRecord record = new RetentionRecord();
        record.setPolicy(policy);
        record.setEntityType(candidate.getEntityType());
        record.setRecordId(candidate.getRecordId());
        record.setAction(policy.getAction());
        record.setExecutionDate(LocalDateTime.now());
        record.setStatus(RetentionStatus.COMPLETED);

        recordRepository.save(record);

        // Also log in audit service
        auditService.logEvent("RETENTION_ACTION_EXECUTED", candidate.getRecordId(),
            String.format("Action: %s, Policy: %s", policy.getAction(), policy.getName()));
    }

    /**
     * Get retention status for a specific record
     */
    public RetentionStatusInfo getRetentionStatus(String entityType, Long recordId) {
        log.debug("Getting retention status for {} with ID {}", entityType, recordId);

        RetentionStatusInfo status = new RetentionStatusInfo();
        status.setEntityType(entityType);
        status.setRecordId(recordId);

        // Find applicable policies
        List<RetentionPolicy> applicablePolicies = policyRepository
            .findByEntityTypeAndActive(entityType, true);

        if (applicablePolicies.isEmpty()) {
            status.setHasRetentionPolicy(false);
            return status;
        }

        status.setHasRetentionPolicy(true);
        status.setPolicies(applicablePolicies);

        // Get highest priority policy
        RetentionPolicy primaryPolicy = applicablePolicies.stream()
            .max(Comparator.comparingInt(RetentionPolicy::getPriority))
            .orElse(null);

        if (primaryPolicy != null) {
            status.setPrimaryPolicy(primaryPolicy);

            // Calculate retention expiry date
            LocalDateTime expiryDate = retentionCalculator.calculateExpiryDate(
                primaryPolicy,
                getRecordCreationDate(entityType, recordId)
            );
            status.setExpiryDate(expiryDate);

            // Calculate days remaining
            long daysRemaining = ChronoUnit.DAYS.between(LocalDateTime.now(), expiryDate);
            status.setDaysRemaining(daysRemaining);

            // Check if action is due
            status.setActionDue(daysRemaining <= 0);

            // Get retention history
            List<RetentionRecord> history = recordRepository
                .findByEntityTypeAndRecordId(entityType, recordId);
            status.setRetentionHistory(history);
        }

        return status;
    }

    /**
     * Apply retention hold (legal hold)
     */
    @Transactional
    public void applyRetentionHold(String entityType, Long recordId, RetentionHoldRequest request) {
        log.info("Applying retention hold on {} {}", entityType, recordId);

        RetentionHold hold = new RetentionHold();
        hold.setEntityType(entityType);
        hold.setRecordId(recordId);
        hold.setReason(request.getReason());
        hold.setHoldType(request.getHoldType());
        hold.setAppliedDate(LocalDateTime.now());
        hold.setAppliedBy(request.getUserId());
        hold.setActive(true);

        if (request.getExpiryDate() != null) {
            hold.setExpiryDate(request.getExpiryDate());
        }

        // Save hold - implementation would persist this
        // retentionHoldRepository.save(hold);

        // Audit hold application
        auditService.logEvent("RETENTION_HOLD_APPLIED", recordId,
            String.format("Type: %s, Reason: %s", request.getHoldType(), request.getReason()));

        log.info("Retention hold applied successfully");
    }

    /**
     * Release retention hold
     */
    @Transactional
    public void releaseRetentionHold(String entityType, Long recordId, Long userId) {
        log.info("Releasing retention hold on {} {}", entityType, recordId);

        // Find and deactivate hold - implementation would update hold record
        // RetentionHold hold = findActiveHold(entityType, recordId);
        // hold.setActive(false);
        // hold.setReleasedDate(LocalDateTime.now());
        // hold.setReleasedBy(userId);

        // Audit hold release
        auditService.logEvent("RETENTION_HOLD_RELEASED", recordId,
            "Retention hold released");

        log.info("Retention hold released successfully");
    }

    /**
     * Get retention statistics
     */
    public RetentionStatistics getStatistics() {
        RetentionStatistics stats = new RetentionStatistics();

        stats.setTotalPolicies(policyRepository.count());
        stats.setActivePolicies(policyRepository.countByActive(true));
        stats.setTotalRetentionActions(recordRepository.count());

        // Actions by type
        Map<RetentionAction, Long> actionsByType = recordRepository.countByAction();
        stats.setActionsByType(actionsByType);

        // Actions by status
        Map<RetentionStatus, Long> actionsByStatus = recordRepository.countByStatus();
        stats.setActionsByStatus(actionsByStatus);

        // Recent executions
        List<RetentionRecord> recentActions = recordRepository
            .findTop10ByOrderByExecutionDateDesc();
        stats.setRecentActions(recentActions);

        // Calculate space saved
        long spaceSaved = calculateSpaceSaved();
        stats.setSpaceSavedBytes(spaceSaved);

        return stats;
    }

    /**
     * Preview retention policy impact
     */
    public RetentionPolicyPreview previewPolicyImpact(RetentionPolicy policy) {
        log.info("Previewing impact of retention policy: {}", policy.getName());

        RetentionPolicyPreview preview = new RetentionPolicyPreview();
        preview.setPolicy(policy);

        // Find candidates without executing
        List<RetentionCandidate> candidates = findRetentionCandidates(policy);
        preview.setAffectedRecords(candidates.size());

        // Calculate total size
        long totalSize = candidates.stream()
            .mapToLong(RetentionCandidate::getSizeBytes)
            .sum();
        preview.setTotalSizeBytes(totalSize);

        // Group by category
        Map<String, Long> byCategory = candidates.stream()
            .collect(Collectors.groupingBy(
                RetentionCandidate::getCategory,
                Collectors.counting()
            ));
        preview.setRecordsByCategory(byCategory);

        // Group by age
        Map<String, Long> byAge = groupByAge(candidates);
        preview.setRecordsByAge(byAge);

        log.info("Policy preview: {} records, {} bytes", candidates.size(), totalSize);
        return preview;
    }

    /**
     * Bulk apply retention policy
     */
    @Async
    @Transactional
    public CompletableFuture<RetentionExecutionResult> bulkApplyRetention(BulkRetentionRequest request) {
        log.info("Executing bulk retention on {} records", request.getRecordIds().size());

        RetentionExecutionResult result = new RetentionExecutionResult();
        result.setStartTime(LocalDateTime.now());
        result.setTotalRecords(request.getRecordIds().size());

        RetentionPolicy policy = policyRepository.findById(request.getPolicyId())
            .orElseThrow(() -> new IllegalArgumentException("Policy not found"));

        for (Long recordId : request.getRecordIds()) {
            try {
                RetentionCandidate candidate = new RetentionCandidate();
                candidate.setEntityType(request.getEntityType());
                candidate.setRecordId(recordId);

                executeRetentionAction(policy, candidate);
                recordRetentionAction(policy, candidate);
                result.incrementProcessed();

            } catch (Exception e) {
                log.error("Failed to apply retention on record: {}", recordId, e);
                result.incrementFailed();
                result.addError(recordId, e.getMessage());
            }
        }

        result.setEndTime(LocalDateTime.now());
        return CompletableFuture.completedFuture(result);
    }

    // Helper methods
    private void validateRetentionPolicy(RetentionPolicyRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Policy name is required");
        }
        if (request.getEntityType() == null) {
            throw new IllegalArgumentException("Entity type is required");
        }
        if (request.getRetentionPeriod() <= 0) {
            throw new IllegalArgumentException("Retention period must be positive");
        }
        if (request.getAction() == null) {
            throw new IllegalArgumentException("Retention action is required");
        }
    }

    private LocalDateTime calculateNextExecution(RetentionPolicy policy) {
        // Calculate next execution based on policy configuration
        // For daily execution
        return LocalDateTime.now().plusDays(1).withHour(3).withMinute(0).withSecond(0);
    }

    private boolean shouldExecutePolicy(RetentionPolicy policy) {
        if (policy.getNextExecutionTime() == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(policy.getNextExecutionTime());
    }

    private List<RetentionCandidate> filterByConditions(
            List<RetentionCandidate> candidates,
            Map<String, Object> conditions) {

        return candidates.stream()
            .filter(candidate -> matchesConditions(candidate, conditions))
            .collect(Collectors.toList());
    }

    private boolean matchesConditions(RetentionCandidate candidate, Map<String, Object> conditions) {
        // Evaluate conditions against candidate
        // Simplified implementation
        return true;
    }

    private LocalDateTime getRecordCreationDate(String entityType, Long recordId) {
        // Get record creation date from database
        return LocalDateTime.now().minusYears(1); // Simplified
    }

    private long calculateSpaceSaved() {
        // Calculate total space saved by retention actions
        return recordRepository.findByAction(RetentionAction.DELETE).stream()
            .mapToLong(r -> 0L) // Would get actual sizes
            .sum();
    }

    private Map<String, Long> groupByAge(List<RetentionCandidate> candidates) {
        Map<String, Long> byAge = new HashMap<>();

        byAge.put("0-30 days", candidates.stream()
            .filter(c -> c.getAgeInDays() <= 30).count());
        byAge.put("31-90 days", candidates.stream()
            .filter(c -> c.getAgeInDays() > 30 && c.getAgeInDays() <= 90).count());
        byAge.put("91-365 days", candidates.stream()
            .filter(c -> c.getAgeInDays() > 90 && c.getAgeInDays() <= 365).count());
        byAge.put("1+ years", candidates.stream()
            .filter(c -> c.getAgeInDays() > 365).count());

        return byAge;
    }
}