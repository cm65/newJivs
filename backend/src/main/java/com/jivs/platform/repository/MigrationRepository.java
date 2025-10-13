package com.jivs.platform.repository;

import com.jivs.platform.domain.migration.Migration;
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
 * Maps to migration_projects table
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
     * Find migrations by project type
     */
    List<Migration> findByProjectType(String projectType);

    /**
     * Find migrations by priority
     */
    List<Migration> findByPriority(String priority);

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
    List<Migration> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find active migrations (in progress or paused)
     */
    @Query("SELECT m FROM Migration m WHERE m.status IN ('IN_PROGRESS', 'PAUSED')")
    List<Migration> findActiveMigrations();

    /**
     * Find failed migrations
     */
    @Query("SELECT m FROM Migration m WHERE m.status = 'FAILED' ORDER BY m.createdAt DESC")
    List<Migration> findFailedMigrations();

    /**
     * Find completed migrations
     */
    @Query("SELECT m FROM Migration m WHERE m.status = 'COMPLETED' ORDER BY m.createdAt DESC")
    List<Migration> findCompletedMigrations();

    /**
     * Count migrations by status
     */
    long countByStatus(MigrationStatus status);

    /**
     * Find migration by project code
     */
    Optional<Migration> findByProjectCode(String projectCode);

    /**
     * Find migration by name
     */
    Optional<Migration> findByName(String name);

    /**
     * Check if migration name exists
     */
    boolean existsByName(String name);

    /**
     * Check if project code exists
     */
    boolean existsByProjectCode(String projectCode);

    /**
     * Find recent migrations
     */
    @Query("SELECT m FROM Migration m ORDER BY m.createdAt DESC")
    Page<Migration> findRecentMigrations(Pageable pageable);

    /**
     * Get migration statistics - simplified version
     * Note: Migration entity maps to migration_projects, not migration_jobs
     * So we can only count projects, not records
     */
    @Query("SELECT COUNT(m) FROM Migration m WHERE m.status = :status")
    long getMigrationCountByStatus(@Param("status") MigrationStatus status);
}
