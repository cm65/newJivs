package com.jivs.platform.repository;

import com.jivs.platform.domain.compliance.DataPortabilityRequest;
import com.jivs.platform.domain.compliance.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for DataPortabilityRequest entity
 */
@Repository
public interface DataPortabilityRequestRepository extends JpaRepository<DataPortabilityRequest, Long> {

    /**
     * Find all requests by user
     */
    List<DataPortabilityRequest> findByUserId(Long userId);

    /**
     * Find requests by status
     */
    List<DataPortabilityRequest> findByStatus(RequestStatus status);

    /**
     * Find requests by user and status
     */
    List<DataPortabilityRequest> findByUserIdAndStatus(Long userId, RequestStatus status);

    /**
     * Find requests by requester email
     */
    List<DataPortabilityRequest> findByRequesterEmail(String email);

    /**
     * Find requests by export format
     */
    List<DataPortabilityRequest> findByExportFormat(String format);

    /**
     * Find request by download token
     */
    Optional<DataPortabilityRequest> findByDownloadToken(String token);

    /**
     * Find requests with expired downloads
     */
    @Query("SELECT dpr FROM DataPortabilityRequest dpr WHERE dpr.downloadExpiresAt < :now " +
           "AND dpr.status = 'COMPLETED'")
    List<DataPortabilityRequest> findExpiredDownloads(@Param("now") LocalDateTime now);

    /**
     * Find requests created within date range
     */
    List<DataPortabilityRequest> findByCreatedDateBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find completed requests with available downloads
     */
    @Query("SELECT dpr FROM DataPortabilityRequest dpr WHERE dpr.status = 'COMPLETED' " +
           "AND dpr.downloadToken IS NOT NULL AND dpr.downloadExpiresAt > :now " +
           "AND dpr.downloadCount < dpr.maxDownloads")
    List<DataPortabilityRequest> findAvailableDownloads(@Param("now") LocalDateTime now);

    /**
     * Find requests targeting specific system
     */
    List<DataPortabilityRequest> findByTargetSystem(String targetSystem);

    /**
     * Count requests by status
     */
    long countByStatus(RequestStatus status);

    /**
     * Count requests by export format
     */
    long countByExportFormat(String format);

    /**
     * Sum of all exported records
     */
    @Query("SELECT COALESCE(SUM(dpr.recordsExported), 0) FROM DataPortabilityRequest dpr " +
           "WHERE dpr.status = 'COMPLETED'")
    long countTotalRecordsExported();
}
