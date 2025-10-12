package com.jivs.platform.repository;

import com.jivs.platform.domain.retention.RetentionAction;
import com.jivs.platform.domain.retention.RetentionPolicy;
import com.jivs.platform.domain.retention.RetentionRecord;
import com.jivs.platform.domain.retention.RetentionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Repository for RetentionRecord entity
 */
@Repository
public interface RetentionRecordRepository extends JpaRepository<RetentionRecord, Long> {

    /**
     * Find records by policy
     */
    List<RetentionRecord> findByPolicy(RetentionPolicy policy);

    /**
     * Find records by status
     */
    List<RetentionRecord> findByStatus(RetentionStatus status);

    /**
     * Find records eligible for action
     */
    List<RetentionRecord> findByStatusAndEligibleForActionAtBefore(RetentionStatus status, LocalDateTime date);

    /**
     * Find records on legal hold
     */
    List<RetentionRecord> findByOnLegalHold(boolean onLegalHold);

    /**
     * Find records by record ID and dataset type
     */
    List<RetentionRecord> findByRecordIdAndDatasetType(Long recordId, String datasetType);

    /**
     * Find records by entity type and record ID
     */
    List<RetentionRecord> findByEntityTypeAndRecordId(String entityType, Long recordId);

    /**
     * Find records by action
     */
    List<RetentionRecord> findByAction(RetentionAction action);

    /**
     * Count records by action
     */
    @Query("SELECT r.action, COUNT(r) FROM RetentionRecord r GROUP BY r.action")
    Map<RetentionAction, Long> countByAction();

    /**
     * Count records by status
     */
    @Query("SELECT r.status, COUNT(r) FROM RetentionRecord r GROUP BY r.status")
    Map<RetentionStatus, Long> countByStatus();

    /**
     * Find top 10 recent records ordered by execution date descending
     */
    List<RetentionRecord> findTop10ByOrderByExecutionDateDesc();
}
