package com.jivs.platform.service.retention;

import com.jivs.platform.service.storage.StorageService;
import com.jivs.platform.service.notification.NotificationService;
import com.jivs.platform.service.notification.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Executes retention actions on records
 */
@Component
@RequiredArgsConstructor
public class RetentionExecutor {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RetentionExecutor.class);

    private final JdbcTemplate jdbcTemplate;
    private final StorageService storageService;
    private final NotificationService notificationService;

    /**
     * Find retention candidates
     */
    public List<RetentionCandidate> findCandidates(
            String entityType,
            LocalDateTime cutoffDate,
            Map<String, Object> conditions,
            String scope) {

        List<RetentionCandidate> candidates = new ArrayList<>();

        switch (entityType.toUpperCase()) {
            case "BUSINESS_OBJECT":
                candidates = findBusinessObjectCandidates(cutoffDate, conditions, scope);
                break;
            case "EXTRACTION":
                candidates = findExtractionCandidates(cutoffDate, conditions, scope);
                break;
            case "MIGRATION":
                candidates = findMigrationCandidates(cutoffDate, conditions, scope);
                break;
            case "DOCUMENT":
                candidates = findDocumentCandidates(cutoffDate, conditions, scope);
                break;
            default:
                log.warn("Unknown entity type for retention: {}", entityType);
        }

        return candidates;
    }

    /**
     * Delete record permanently
     */
    public void deleteRecord(RetentionCandidate candidate) {
        log.info("Deleting record: {} {}", candidate.getEntityType(), candidate.getRecordId());

        String tableName = getTableName(candidate.getEntityType());
        String sql = String.format("DELETE FROM %s WHERE id = ?", tableName);

        // Delete associated files if any
        deleteAssociatedFiles(candidate);

        // Execute deletion
        jdbcTemplate.update(sql, candidate.getRecordId());

        log.info("Record deleted successfully");
    }

    /**
     * Archive record
     */
    public void archiveRecord(RetentionCandidate candidate) {
        log.info("Archiving record: {} {}", candidate.getEntityType(), candidate.getRecordId());

        // Update record status
        String tableName = getTableName(candidate.getEntityType());
        String sql = String.format(
            "UPDATE %s SET status = 'ARCHIVED', archived_date = ? WHERE id = ?",
            tableName
        );

        jdbcTemplate.update(sql, LocalDateTime.now(), candidate.getRecordId());

        log.info("Record archived successfully");
    }

    /**
     * Move to cold storage
     */
    public void moveToColdStorage(RetentionCandidate candidate) {
        log.info("Moving to cold storage: {} {}", candidate.getEntityType(), candidate.getRecordId());

        // Update record metadata
        String tableName = getTableName(candidate.getEntityType());
        String sql = String.format(
            "UPDATE %s SET storage_tier = 'COLD', moved_to_cold_date = ? WHERE id = ?",
            tableName
        );

        jdbcTemplate.update(sql, LocalDateTime.now(), candidate.getRecordId());

        log.info("Record moved to cold storage successfully");
    }

    /**
     * Anonymize record (GDPR compliance)
     */
    public void anonymizeRecord(RetentionCandidate candidate) {
        log.info("Anonymizing record: {} {}", candidate.getEntityType(), candidate.getRecordId());

        // Identify PII fields
        List<String> piiFields = identifyPIIFields(candidate.getEntityType());

        // Anonymize each PII field
        String tableName = getTableName(candidate.getEntityType());
        for (String field : piiFields) {
            String sql = String.format(
                "UPDATE %s SET %s = '[REDACTED]' WHERE id = ?",
                tableName, field
            );
            jdbcTemplate.update(sql, candidate.getRecordId());
        }

        // Mark as anonymized
        String statusSql = String.format(
            "UPDATE %s SET anonymized = true, anonymized_date = ? WHERE id = ?",
            tableName
        );
        jdbcTemplate.update(statusSql, LocalDateTime.now(), candidate.getRecordId());

        log.info("Record anonymized successfully");
    }

    /**
     * Soft delete record
     */
    public void softDeleteRecord(RetentionCandidate candidate) {
        log.info("Soft deleting record: {} {}", candidate.getEntityType(), candidate.getRecordId());

        String tableName = getTableName(candidate.getEntityType());
        String sql = String.format(
            "UPDATE %s SET deleted = true, deleted_date = ? WHERE id = ?",
            tableName
        );

        jdbcTemplate.update(sql, LocalDateTime.now(), candidate.getRecordId());

        log.info("Record soft deleted successfully");
    }

    /**
     * Send notification about retention due
     */
    public void notifyRetentionDue(RetentionCandidate candidate) {
        log.info("Notifying retention due: {} {}", candidate.getEntityType(), candidate.getRecordId());

        // Build notification message
        String message = String.format(
            "Retention action is due for %s with ID %d. " +
            "Record was created %d days ago.",
            candidate.getEntityType(),
            candidate.getRecordId(),
            candidate.getAgeInDays()
        );

        // Send notification to stakeholders
        List<String> stakeholders = getStakeholders(candidate);
        for (String stakeholder : stakeholders) {
            NotificationRequest request = new NotificationRequest();
            request.setUserId("1"); // Admin user - would need to lookup actual user ID
            request.setType(com.jivs.platform.service.notification.NotificationType.RETENTION_ACTION_DUE);
            request.setSubject("Retention Action Due");
            request.setMessage(message);
            request.setPriority(com.jivs.platform.service.notification.NotificationPriority.HIGH);

            notificationService.sendNotification(request);
        }

        log.info("Retention notification sent successfully");
    }

    // Helper methods
    private List<RetentionCandidate> findBusinessObjectCandidates(
            LocalDateTime cutoffDate,
            Map<String, Object> conditions,
            String scope) {

        String sql = "SELECT id, created_date, name FROM business_objects " +
                    "WHERE created_date < ? AND status NOT IN ('ARCHIVED', 'DELETED')";

        return jdbcTemplate.query(sql, new Object[]{cutoffDate}, (rs, rowNum) -> {
            RetentionCandidate candidate = new RetentionCandidate();
            candidate.setEntityType("BUSINESS_OBJECT");
            candidate.setRecordId(rs.getLong("id"));
            candidate.setCreatedDate(rs.getTimestamp("created_date").toLocalDateTime());
            candidate.setCategory(rs.getString("name"));
            return candidate;
        });
    }

    private List<RetentionCandidate> findExtractionCandidates(
            LocalDateTime cutoffDate,
            Map<String, Object> conditions,
            String scope) {

        String sql = "SELECT id, created_date, source_system FROM extraction_jobs " +
                    "WHERE created_date < ? AND status = 'COMPLETED'";

        return jdbcTemplate.query(sql, new Object[]{cutoffDate}, (rs, rowNum) -> {
            RetentionCandidate candidate = new RetentionCandidate();
            candidate.setEntityType("EXTRACTION");
            candidate.setRecordId(rs.getLong("id"));
            candidate.setCreatedDate(rs.getTimestamp("created_date").toLocalDateTime());
            candidate.setCategory(rs.getString("source_system"));
            return candidate;
        });
    }

    private List<RetentionCandidate> findMigrationCandidates(
            LocalDateTime cutoffDate,
            Map<String, Object> conditions,
            String scope) {

        String sql = "SELECT id, created_date, migration_type FROM migrations " +
                    "WHERE created_date < ? AND status IN ('COMPLETED', 'FAILED')";

        return jdbcTemplate.query(sql, new Object[]{cutoffDate}, (rs, rowNum) -> {
            RetentionCandidate candidate = new RetentionCandidate();
            candidate.setEntityType("MIGRATION");
            candidate.setRecordId(rs.getLong("id"));
            candidate.setCreatedDate(rs.getTimestamp("created_date").toLocalDateTime());
            candidate.setCategory(rs.getString("migration_type"));
            return candidate;
        });
    }

    private List<RetentionCandidate> findDocumentCandidates(
            LocalDateTime cutoffDate,
            Map<String, Object> conditions,
            String scope) {

        String sql = "SELECT id, created_date, document_type FROM documents " +
                    "WHERE created_date < ? AND status != 'ARCHIVED'";

        return jdbcTemplate.query(sql, new Object[]{cutoffDate}, (rs, rowNum) -> {
            RetentionCandidate candidate = new RetentionCandidate();
            candidate.setEntityType("DOCUMENT");
            candidate.setRecordId(rs.getLong("id"));
            candidate.setCreatedDate(rs.getTimestamp("created_date").toLocalDateTime());
            candidate.setCategory(rs.getString("document_type"));
            return candidate;
        });
    }

    private String getTableName(String entityType) {
        switch (entityType.toUpperCase()) {
            case "BUSINESS_OBJECT": return "business_objects";
            case "EXTRACTION": return "extraction_jobs";
            case "MIGRATION": return "migrations";
            case "DOCUMENT": return "documents";
            default: throw new IllegalArgumentException("Unknown entity type: " + entityType);
        }
    }

    private void deleteAssociatedFiles(RetentionCandidate candidate) {
        // Delete files associated with the record
        // Note: Implementation would query for associated file storage IDs
        // and call storageService.deleteFile(storageId) for each
        log.debug("Deleting associated files for: {} {}", candidate.getEntityType(), candidate.getRecordId());
    }

    private List<String> identifyPIIFields(String entityType) {
        // Return list of PII fields for the entity type
        Map<String, List<String>> piiFieldsMap = new HashMap<>();
        piiFieldsMap.put("BUSINESS_OBJECT", Arrays.asList("owner_email", "contact_info"));
        piiFieldsMap.put("DOCUMENT", Arrays.asList("author", "email", "phone"));
        piiFieldsMap.put("USER", Arrays.asList("email", "phone", "address", "ssn"));

        return piiFieldsMap.getOrDefault(entityType, new ArrayList<>());
    }

    private List<String> getStakeholders(RetentionCandidate candidate) {
        // Get list of stakeholders who should be notified
        return Arrays.asList("admin@jivs.com", "compliance@jivs.com");
    }
}