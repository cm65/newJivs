package com.jivs.platform.repository;

import com.jivs.platform.domain.compliance.DataSubjectAccessRequest;
import com.jivs.platform.domain.compliance.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for DataSubjectAccessRequest entity
 */
@Repository
public interface DataSubjectAccessRequestRepository extends JpaRepository<DataSubjectAccessRequest, Long> {

    /**
     * Find all requests by user
     */
    List<DataSubjectAccessRequest> findByUserId(Long userId);

    /**
     * Find requests by status
     */
    List<DataSubjectAccessRequest> findByStatus(RequestStatus status);

    /**
     * Find requests by user and status
     */
    List<DataSubjectAccessRequest> findByUserIdAndStatus(Long userId, RequestStatus status);

    /**
     * Find requests by requester email
     */
    List<DataSubjectAccessRequest> findByRequesterEmail(String email);

    /**
     * Find request by download token
     */
    Optional<DataSubjectAccessRequest> findByDownloadToken(String token);

    /**
     * Find requests with expired downloads
     */
    @Query("SELECT dsar FROM DataSubjectAccessRequest dsar WHERE dsar.downloadExpiresAt < :now " +
           "AND dsar.status = 'COMPLETED'")
    List<DataSubjectAccessRequest> findExpiredDownloads(@Param("now") LocalDateTime now);

    /**
     * Find requests created within date range
     */
    List<DataSubjectAccessRequest> findByCreatedDateBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find completed requests with available downloads
     */
    @Query("SELECT dsar FROM DataSubjectAccessRequest dsar WHERE dsar.status = 'COMPLETED' " +
           "AND dsar.downloadToken IS NOT NULL AND dsar.downloadExpiresAt > :now " +
           "AND dsar.downloadCount < dsar.maxDownloads")
    List<DataSubjectAccessRequest> findAvailableDownloads(@Param("now") LocalDateTime now);

    /**
     * Count requests by status
     */
    long countByStatus(RequestStatus status);

    /**
     * Count requests by user
     */
    long countByUserId(Long userId);
}
