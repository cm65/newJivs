package com.jivs.platform.repository;

import com.jivs.platform.domain.transformation.TransformationJob;
import com.jivs.platform.domain.transformation.TransformationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TransformationJob entity
 */
@Repository
public interface TransformationJobRepository extends JpaRepository<TransformationJob, Long> {

    /**
     * Find transformation job by job ID
     */
    Optional<TransformationJob> findByJobId(String jobId);

    /**
     * Find transformation jobs by status
     */
    List<TransformationJob> findByStatus(TransformationStatus status);

    /**
     * Find transformation jobs by status with pagination
     */
    Page<TransformationJob> findByStatus(TransformationStatus status, Pageable pageable);

    /**
     * Find transformation jobs by source system
     */
    List<TransformationJob> findBySourceSystem(String sourceSystem);

    /**
     * Find transformation jobs by target system
     */
    List<TransformationJob> findByTargetSystem(String targetSystem);

    /**
     * Find transformation jobs by source and target systems
     */
    List<TransformationJob> findBySourceSystemAndTargetSystem(String sourceSystem, String targetSystem);

    /**
     * Find transformation jobs created by user
     */
    List<TransformationJob> findByCreatedBy(String createdBy);

    /**
     * Find transformation jobs created between dates
     */
    List<TransformationJob> findByCreatedDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find active transformation jobs (in progress)
     */
    @Query("SELECT t FROM TransformationJob t WHERE t.status = 'IN_PROGRESS'")
    List<TransformationJob> findActiveJobs();

    /**
     * Find failed transformation jobs
     */
    @Query("SELECT t FROM TransformationJob t WHERE t.status = 'FAILED' ORDER BY t.createdDate DESC")
    List<TransformationJob> findFailedJobs();

    /**
     * Find completed transformation jobs in date range
     */
    @Query("SELECT t FROM TransformationJob t WHERE t.status = 'COMPLETED' " +
           "AND t.completionTime BETWEEN :startDate AND :endDate")
    List<TransformationJob> findCompletedJobsBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count transformation jobs by status
     */
    long countByStatus(TransformationStatus status);

    /**
     * Find transformation job by name
     */
    Optional<TransformationJob> findByName(String name);

    /**
     * Check if transformation job name exists
     */
    boolean existsByName(String name);

    /**
     * Check if job ID exists
     */
    boolean existsByJobId(String jobId);

    /**
     * Find recent transformation jobs
     */
    @Query("SELECT t FROM TransformationJob t ORDER BY t.createdDate DESC")
    Page<TransformationJob> findRecentJobs(Pageable pageable);

    /**
     * Get transformation job statistics
     */
    @Query("SELECT " +
           "COUNT(t), " +
           "SUM(t.recordsProcessed), " +
           "SUM(t.recordsTransformed), " +
           "SUM(t.recordsFailed) " +
           "FROM TransformationJob t WHERE t.status = :status")
    Object[] getJobStatisticsByStatus(@Param("status") TransformationStatus status);

    /**
     * Find transformation jobs by target format
     */
    List<TransformationJob> findByTargetFormat(String targetFormat);
}
