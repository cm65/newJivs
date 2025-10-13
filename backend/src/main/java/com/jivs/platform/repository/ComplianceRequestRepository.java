package com.jivs.platform.repository;

import com.jivs.platform.domain.compliance.ComplianceRequest;
import com.jivs.platform.domain.compliance.ComplianceRequestType;
import com.jivs.platform.domain.compliance.ComplianceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ComplianceRequest entity
 * Maps to data_subject_requests table
 */
@Repository
public interface ComplianceRequestRepository extends JpaRepository<ComplianceRequest, Long> {

    /**
     * Find requests by status (using correct enum type)
     */
    List<ComplianceRequest> findByStatus(ComplianceStatus status);

    /**
     * Find requests by status with pagination
     */
    Page<ComplianceRequest> findByStatus(ComplianceStatus status, Pageable pageable);

    /**
     * Find requests by type (using correct enum type)
     */
    List<ComplianceRequest> findByRequestType(ComplianceRequestType requestType);

    /**
     * Find requests by subject email
     */
    List<ComplianceRequest> findBySubjectEmail(String email);

    /**
     * Find requests by subject identifier
     */
    List<ComplianceRequest> findBySubjectIdentifier(String identifier);

    /**
     * Find requests by request ID
     */
    Optional<ComplianceRequest> findByRequestId(String requestId);

    /**
     * Find overdue requests (using LocalDate for dueDate)
     */
    @Query("SELECT cr FROM ComplianceRequest cr WHERE cr.dueDate < :now " +
           "AND cr.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<ComplianceRequest> findOverdueRequests(@Param("now") LocalDate now);

    /**
     * Find requests created within date range (using createdAt)
     */
    List<ComplianceRequest> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find unverified requests older than specified date (using createdAt)
     */
    @Query("SELECT cr FROM ComplianceRequest cr WHERE cr.verifiedAt IS NULL " +
           "AND cr.createdAt < :cutoffDate AND cr.status = 'PENDING'")
    List<ComplianceRequest> findUnverifiedRequestsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Count requests by status
     */
    long countByStatus(ComplianceStatus status);

    /**
     * Find requests due within specified date range (using LocalDate)
     */
    @Query("SELECT cr FROM ComplianceRequest cr WHERE cr.dueDate BETWEEN :now AND :future " +
           "AND cr.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<ComplianceRequest> findRequestsDueWithin(@Param("now") LocalDate now,
                                                   @Param("future") LocalDate future);

    /**
     * Find requests by status and due date before (using LocalDate)
     */
    List<ComplianceRequest> findByStatusAndDueDateBefore(ComplianceStatus status, LocalDate dueDate);

    /**
     * Find requests by priority
     */
    List<ComplianceRequest> findByPriority(String priority);

    /**
     * Find requests by verification status
     */
    List<ComplianceRequest> findByVerificationStatus(String verificationStatus);

    /**
     * Find requests assigned to specific user
     */
    List<ComplianceRequest> findByAssignedTo(String assignedTo);

    /**
     * Find requests by request source
     */
    List<ComplianceRequest> findByRequestSource(String requestSource);

    /**
     * Find completed requests within date range
     */
    List<ComplianceRequest> findByStatusAndCompletedDateBetween(
            ComplianceStatus status, LocalDate start, LocalDate end);
}
