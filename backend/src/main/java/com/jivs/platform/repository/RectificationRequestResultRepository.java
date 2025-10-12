package com.jivs.platform.repository;

import com.jivs.platform.domain.compliance.RectificationRequestResult;
import com.jivs.platform.domain.compliance.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for RectificationRequestResult entity
 */
@Repository
public interface RectificationRequestResultRepository extends JpaRepository<RectificationRequestResult, Long> {

    /**
     * Find all results by user
     */
    List<RectificationRequestResult> findByUserId(Long userId);

    /**
     * Find results by status
     */
    List<RectificationRequestResult> findByStatus(RequestStatus status);

    /**
     * Find results by user and status
     */
    List<RectificationRequestResult> findByUserIdAndStatus(Long userId, RequestStatus status);

    /**
     * Find results by requester email
     */
    List<RectificationRequestResult> findByRequesterEmail(String email);

    /**
     * Find results created within date range
     */
    List<RectificationRequestResult> findByCreatedDateBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find completed results
     */
    @Query("SELECT rrr FROM RectificationRequestResult rrr WHERE rrr.status = 'COMPLETED' " +
           "AND rrr.completedAt BETWEEN :start AND :end")
    List<RectificationRequestResult> findCompletedBetween(@Param("start") LocalDateTime start,
                                                           @Param("end") LocalDateTime end);

    /**
     * Find verified results
     */
    @Query("SELECT rrr FROM RectificationRequestResult rrr WHERE rrr.verifiedAt IS NOT NULL")
    List<RectificationRequestResult> findVerifiedResults();

    /**
     * Find unverified results
     */
    @Query("SELECT rrr FROM RectificationRequestResult rrr WHERE rrr.verifiedAt IS NULL " +
           "AND rrr.status = 'COMPLETED'")
    List<RectificationRequestResult> findUnverifiedResults();

    /**
     * Count results by status
     */
    long countByStatus(RequestStatus status);

    /**
     * Sum of all rectified fields
     */
    @Query("SELECT COALESCE(SUM(rrr.fieldsRectified), 0) FROM RectificationRequestResult rrr " +
           "WHERE rrr.status = 'COMPLETED'")
    long countTotalFieldsRectified();

    /**
     * Sum of all rejected fields
     */
    @Query("SELECT COALESCE(SUM(rrr.fieldsRejected), 0) FROM RectificationRequestResult rrr")
    long countTotalFieldsRejected();

    /**
     * Find fully completed rectifications
     */
    @Query("SELECT rrr FROM RectificationRequestResult rrr WHERE rrr.fieldsRequested > 0 " +
           "AND rrr.fieldsRectified = rrr.fieldsRequested")
    List<RectificationRequestResult> findFullyCompleted();
}
