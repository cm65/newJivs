package com.jivs.platform.repository;

import com.jivs.platform.domain.compliance.DataErasureRequest;
import com.jivs.platform.domain.compliance.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for DataErasureRequest entity
 */
@Repository
public interface DataErasureRequestRepository extends JpaRepository<DataErasureRequest, Long> {

    /**
     * Find all requests by user
     */
    List<DataErasureRequest> findByUserId(Long userId);

    /**
     * Find requests by status
     */
    List<DataErasureRequest> findByStatus(RequestStatus status);

    /**
     * Find requests by user and status
     */
    List<DataErasureRequest> findByUserIdAndStatus(Long userId, RequestStatus status);

    /**
     * Find requests by requester email
     */
    List<DataErasureRequest> findByRequesterEmail(String email);

    /**
     * Find requests scheduled for erasure
     */
    @Query("SELECT der FROM DataErasureRequest der WHERE der.scheduledErasureDate <= :date " +
           "AND der.status = 'IN_PROGRESS'")
    List<DataErasureRequest> findScheduledForErasure(@Param("date") LocalDateTime date);

    /**
     * Find requests with full erasure
     */
    List<DataErasureRequest> findByEraseAllData(Boolean eraseAllData);

    /**
     * Find requests with anonymization
     */
    List<DataErasureRequest> findByAnonymizeInsteadOfDelete(Boolean anonymize);

    /**
     * Find requests created within date range
     */
    List<DataErasureRequest> findByCreatedDateBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find completed requests
     */
    @Query("SELECT der FROM DataErasureRequest der WHERE der.status = 'COMPLETED' " +
           "AND der.erasureCompletedAt BETWEEN :start AND :end")
    List<DataErasureRequest> findCompletedBetween(@Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);

    /**
     * Count requests by status
     */
    long countByStatus(RequestStatus status);

    /**
     * Count total records erased
     */
    @Query("SELECT COALESCE(SUM(der.recordsErased), 0) FROM DataErasureRequest der " +
           "WHERE der.status = 'COMPLETED'")
    long countTotalRecordsErased();

    /**
     * Count total records anonymized
     */
    @Query("SELECT COALESCE(SUM(der.recordsAnonymized), 0) FROM DataErasureRequest der " +
           "WHERE der.status = 'COMPLETED'")
    long countTotalRecordsAnonymized();
}
