package com.jivs.platform.repository;

import com.jivs.platform.domain.extraction.ExtractionConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ExtractionConfig entity
 */
@Repository
public interface ExtractionConfigRepository extends JpaRepository<ExtractionConfig, Long> {

    /**
     * Find extraction config by name
     */
    Optional<ExtractionConfig> findByName(String name);

    /**
     * Find all enabled extraction configs
     */
    List<ExtractionConfig> findByIsEnabledTrue();

    /**
     * Find extraction configs by data source
     */
    Page<ExtractionConfig> findByDataSourceId(Long dataSourceId, Pageable pageable);

    /**
     * Find extraction configs with data source (avoid N+1)
     */
    @Query("SELECT ec FROM ExtractionConfig ec JOIN FETCH ec.dataSource")
    List<ExtractionConfig> findAllWithDataSource();

    /**
     * Count enabled extraction configs
     */
    long countByIsEnabledTrue();
}
