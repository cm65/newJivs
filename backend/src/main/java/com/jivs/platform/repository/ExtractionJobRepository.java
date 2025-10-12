package com.jivs.platform.repository;

import com.jivs.platform.domain.extraction.ExtractionJob;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * P0.4: Repository for ExtractionJob entity with optimized queries
 *
 * Optimizations:
 * - JOIN FETCH to eliminate N+1 queries
 * - Batch status updates
 * - Cached statistics queries
 *
 * Expected Impact:
 * - Throughput: +5% (19.25k → 20.2k records/min)
 * - Latency: -20ms (220ms → 200ms)
 */
@Repository
public interface ExtractionJobRepository extends JpaRepository<ExtractionJob, Long> {

    /**
     * P0.4: Find by jobId with eager loading of DataSource
     * Eliminates N+1 query problem
     */
    @Query("SELECT e FROM ExtractionJob e " +
           "LEFT JOIN FETCH e.dataSource " +
           "WHERE e.jobId = :jobId")
    Optional<ExtractionJob> findByJobIdWithDataSource(@Param("jobId") String jobId);

    /**
     * Original findByJobId - kept for backward compatibility
     */
    Optional<ExtractionJob> findByJobId(String jobId);

    List<ExtractionJob> findByStatus(ExtractionJob.JobStatus status);

    Page<ExtractionJob> findByDataSourceId(Long dataSourceId, Pageable pageable);

    @Query("SELECT e FROM ExtractionJob e WHERE e.startTime >= :startTime AND e.startTime <= :endTime")
    List<ExtractionJob> findByDateRange(@Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * P0.4: Count by status with caching (5 minutes TTL)
     */
    @Cacheable(value = "extractionStats", key = "'count:' + #status.name()")
    @Query("SELECT COUNT(e) FROM ExtractionJob e WHERE e.status = :status")
    Long countByStatus(@Param("status") ExtractionJob.JobStatus status);

    /**
     * P0.4: Find running jobs with eager loading of DataSource
     * Eliminates N+1 queries when checking running jobs
     */
    @Query("SELECT e FROM ExtractionJob e " +
           "LEFT JOIN FETCH e.dataSource " +
           "WHERE e.status = 'RUNNING' " +
           "ORDER BY e.startTime DESC")
    List<ExtractionJob> findRunningJobsWithDataSource();

    /**
     * Original findRunningJobs - kept for backward compatibility
     */
    @Query("SELECT e FROM ExtractionJob e WHERE e.status = 'RUNNING' ORDER BY e.startTime DESC")
    List<ExtractionJob> findRunningJobs();

    /**
     * P0.4: Batch status update for multiple jobs
     * Reduces database writes from N to 1 transaction
     */
    @Modifying
    @Query("UPDATE ExtractionJob e SET e.status = :status, e.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE e.id IN :ids")
    void updateStatusBatch(@Param("ids") List<Long> ids, @Param("status") ExtractionJob.JobStatus status);

    /**
     * P0.4: Find jobs by status with DataSource eager loading
     */
    @Query("SELECT e FROM ExtractionJob e " +
           "LEFT JOIN FETCH e.dataSource " +
           "WHERE e.status = :status " +
           "ORDER BY e.createdAt DESC")
    List<ExtractionJob> findByStatusWithDataSource(@Param("status") ExtractionJob.JobStatus status);

    /**
     * P0.4: Find recent jobs with pagination and eager loading
     */
    @Query(value = "SELECT e FROM ExtractionJob e " +
                   "LEFT JOIN FETCH e.dataSource " +
                   "ORDER BY e.createdAt DESC",
           countQuery = "SELECT COUNT(e) FROM ExtractionJob e")
    Page<ExtractionJob> findAllWithDataSource(Pageable pageable);
}