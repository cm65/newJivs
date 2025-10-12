package com.jivs.platform.repository;

import com.jivs.platform.domain.audit.AuditAction;
import com.jivs.platform.domain.audit.AuditLog;
import com.jivs.platform.domain.audit.AuditSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AuditLog entity
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find audit logs by user ID
     */
    List<AuditLog> findByUserId(Long userId);

    /**
     * Find audit logs by action
     */
    List<AuditLog> findByAction(AuditAction action);

    /**
     * Find audit logs by resource (backward compatibility)
     */
    @Deprecated
    default List<AuditLog> findByResourceTypeAndResourceId(String resourceType, String resourceId) {
        return findByEntityTypeAndEntityId(resourceType, resourceId);
    }

    /**
     * Find audit logs by entity
     */
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId);

    /**
     * Find audit logs by entity ID
     */
    List<AuditLog> findByEntityIdOrderByTimestampDesc(String entityId);

    /**
     * Find audit logs by severity
     */
    List<AuditLog> findBySeverity(AuditSeverity severity);

    /**
     * Find audit logs by timestamp range
     */
    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find audit logs before a certain timestamp
     */
    List<AuditLog> findByTimestampBefore(LocalDateTime cutoffDate);

    /**
     * Find audit logs by user and timestamp range
     */
    List<AuditLog> findByUserIdAndTimestampBetweenOrderByTimestampDesc(
        Long userId,
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    /**
     * Find failed operations
     */
    List<AuditLog> findBySuccess(boolean success);

    /**
     * Find recent audit logs
     */
    List<AuditLog> findTop100ByOrderByTimestampDesc();

    /**
     * Search audit logs by criteria
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:eventType IS NULL OR a.eventType = :eventType) AND " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:severity IS NULL OR a.severity = :severity) AND " +
           "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR a.timestamp <= :endDate) AND " +
           "(:entityId IS NULL OR a.entityId = :entityId) " +
           "ORDER BY a.timestamp DESC")
    List<AuditLog> searchByCriteria(
        @Param("eventType") String eventType,
        @Param("userId") Long userId,
        @Param("severity") AuditSeverity severity,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("entityId") String entityId
    );

    /**
     * Delete audit logs before a certain timestamp
     */
    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.timestamp < :cutoffDate")
    int deleteByTimestampBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find gaps in audit sequence (for integrity checking)
     */
    @Query("SELECT a.id FROM AuditLog a WHERE a.id + 1 NOT IN (SELECT b.id FROM AuditLog b)")
    List<Long> findGapsInAuditSequence();

    /**
     * Count audit logs by event type
     */
    @Query("SELECT a.eventType, COUNT(a) FROM AuditLog a GROUP BY a.eventType")
    List<Object[]> countByEventType();

    /**
     * Count audit logs by severity
     */
    @Query("SELECT a.severity, COUNT(a) FROM AuditLog a GROUP BY a.severity")
    List<Object[]> countBySeverity();

    /**
     * Count audit logs by action
     */
    @Query("SELECT a.action, COUNT(a) FROM AuditLog a GROUP BY a.action")
    List<Object[]> countByAction();
}
