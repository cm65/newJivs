package com.jivs.platform.repository;

import com.jivs.platform.domain.compliance.ComplianceRequest;
import com.jivs.platform.domain.compliance.ComplianceStatus;
import com.jivs.platform.domain.compliance.Regulation;
import com.jivs.platform.domain.compliance.RequestStatus;
import com.jivs.platform.domain.compliance.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ComplianceRequest entity
 */
@Repository
public interface ComplianceRequestRepository extends JpaRepository<ComplianceRequest, Long> {

    /**
     * Find all requests by user
     */
    List<ComplianceRequest> findByUserId(Long userId);

    /**
     * Find requests by status
     */
    List<ComplianceRequest> findByStatus(RequestStatus status);

    /**
     * Find requests by type
     */
    List<ComplianceRequest> findByRequestType(RequestType requestType);

    /**
     * Find requests by regulation
     */
    List<ComplianceRequest> findByRegulation(Regulation regulation);

    /**
     * Find requests by user and status
     */
    List<ComplianceRequest> findByUserIdAndStatus(Long userId, RequestStatus status);

    /**
     * Find requests by requester email
     */
    List<ComplianceRequest> findByRequesterEmail(String email);

    /**
     * Find overdue requests
     */
    @Query("SELECT cr FROM ComplianceRequest cr WHERE cr.dueDate < :now " +
           "AND cr.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<ComplianceRequest> findOverdueRequests(@Param("now") LocalDateTime now);

    /**
     * Find requests created within date range
     */
    List<ComplianceRequest> findByCreatedDateBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find requests by verification token
     */
    Optional<ComplianceRequest> findByVerificationToken(String token);

    /**
     * Find unverified requests older than specified date
     */
    @Query("SELECT cr FROM ComplianceRequest cr WHERE cr.verifiedAt IS NULL " +
           "AND cr.createdDate < :cutoffDate AND cr.status = 'PENDING'")
    List<ComplianceRequest> findUnverifiedRequestsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Count requests by status
     */
    long countByStatus(RequestStatus status);

    /**
     * Count requests by user and status
     */
    long countByUserIdAndStatus(Long userId, RequestStatus status);

    /**
     * Find requests due within specified days
     */
    @Query("SELECT cr FROM ComplianceRequest cr WHERE cr.dueDate BETWEEN :now AND :future " +
           "AND cr.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<ComplianceRequest> findRequestsDueWithin(@Param("now") LocalDateTime now,
                                                   @Param("future") LocalDateTime future);

    /**
     * Find requests submitted within date range
     */
    List<ComplianceRequest> findBySubmittedDateBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find requests by status and due date before
     */
    List<ComplianceRequest> findByStatusAndDueDateBefore(ComplianceStatus status, LocalDateTime dueDate);
}
