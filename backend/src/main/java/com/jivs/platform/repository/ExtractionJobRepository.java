package com.jivs.platform.repository;

import com.jivs.platform.domain.extraction.ExtractionJob;
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
 * Repository for ExtractionJob entity
 */
@Repository
public interface ExtractionJobRepository extends JpaRepository<ExtractionJob, Long> {

    Optional<ExtractionJob> findByJobId(String jobId);

    List<ExtractionJob> findByStatus(ExtractionJob.JobStatus status);

    Page<ExtractionJob> findByDataSourceId(Long dataSourceId, Pageable pageable);

    @Query("SELECT e FROM ExtractionJob e WHERE e.startTime >= :startTime AND e.startTime <= :endTime")
    List<ExtractionJob> findByDateRange(@Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(e) FROM ExtractionJob e WHERE e.status = :status")
    Long countByStatus(@Param("status") ExtractionJob.JobStatus status);

    @Query("SELECT e FROM ExtractionJob e WHERE e.status = 'RUNNING' ORDER BY e.startTime DESC")
    List<ExtractionJob> findRunningJobs();
}