package com.jivs.platform.repository;

import com.jivs.platform.domain.migration.Migration;
import com.jivs.platform.domain.migration.MigrationPhase;
import com.jivs.platform.domain.migration.MigrationStatus;
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
 * Repository interface for Migration entity
 */
@Repository
public interface MigrationRepository extends JpaRepository<Migration, Long> {

    /**
     * Find migrations by status
     */
    List<Migration> findByStatus(MigrationStatus status);

    /**
     * Find migrations by status with pagination
     */
    Page<Migration> findByStatus(MigrationStatus status, Pageable pageable);

    /**
     * Find migrations by phase
     */
    List<Migration> findByPhase(MigrationPhase phase);

    /**
     * Find migrations by source system
     */
    List<Migration> findBySourceSystem(String sourceSystem);

    /**
     * Find migrations by target system
     */
    List<Migration> findByTargetSystem(String targetSystem);

    /**
     * Find migrations by source and target systems
     */
    List<Migration> findBySourceSystemAndTargetSystem(String sourceSystem, String targetSystem);

    /**
     * Find migrations created by user
     */
    List<Migration> findByCreatedBy(String createdBy);

    /**
     * Find migrations created between dates
     */
    List<Migration> findByCreatedDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find active migrations (in progress or paused)
     */
    @Query("SELECT m FROM Migration m WHERE m.status IN ('IN_PROGRESS', 'PAUSED')")
    List<Migration> findActiveMigrations();

    /**
     * Find failed migrations
     */
    @Query("SELECT m FROM Migration m WHERE m.status = 'FAILED' ORDER BY m.createdDate DESC")
    List<Migration> findFailedMigrations();

    /**
     * Find completed migrations in date range
     */
    @Query("SELECT m FROM Migration m WHERE m.status = 'COMPLETED' AND m.completionTime BETWEEN :startDate AND :endDate")
    List<Migration> findCompletedMigrationsBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count migrations by status
     */
    long countByStatus(MigrationStatus status);

    /**
     * Find migration by name
     */
    Optional<Migration> findByName(String name);

    /**
     * Check if migration name exists
     */
    boolean existsByName(String name);

    /**
     * Find recent migrations
     */
    @Query("SELECT m FROM Migration m ORDER BY m.createdDate DESC")
    Page<Migration> findRecentMigrations(Pageable pageable);

    /**
     * Get migration statistics
     */
    @Query("SELECT " +
           "COUNT(m), " +
           "SUM(m.metrics.totalRecords), " +
           "SUM(m.metrics.successfulRecords), " +
           "SUM(m.metrics.failedRecords) " +
           "FROM Migration m WHERE m.status = :status")
    Object[] getMigrationStatisticsByStatus(@Param("status") MigrationStatus status);
}
