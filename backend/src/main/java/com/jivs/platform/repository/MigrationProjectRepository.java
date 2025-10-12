package com.jivs.platform.repository;

import com.jivs.platform.domain.migration.MigrationProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for MigrationProject entity
 */
@Repository
public interface MigrationProjectRepository extends JpaRepository<MigrationProject, Long> {

    Optional<MigrationProject> findByProjectCode(String projectCode);

    Boolean existsByProjectCode(String projectCode);

    List<MigrationProject> findByStatus(MigrationProject.ProjectStatus status);

    List<MigrationProject> findByProjectType(MigrationProject.ProjectType projectType);

    Page<MigrationProject> findByPriority(MigrationProject.Priority priority, Pageable pageable);

    @Query("SELECT m FROM MigrationProject m WHERE m.status IN :statuses")
    List<MigrationProject> findByStatusIn(@Param("statuses") List<MigrationProject.ProjectStatus> statuses);

    @Query("SELECT COUNT(m) FROM MigrationProject m WHERE m.status = :status")
    Long countByStatus(@Param("status") MigrationProject.ProjectStatus status);
}