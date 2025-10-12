package com.jivs.platform.repository;

import com.jivs.platform.domain.extraction.DataSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for DataSource entity
 */
@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, Long> {

    List<DataSource> findBySourceType(DataSource.SourceType sourceType);

    List<DataSource> findByIsActiveTrue();

    Page<DataSource> findByIsActive(Boolean isActive, Pageable pageable);

    List<DataSource> findByLastConnectionStatus(DataSource.ConnectionStatus status);
}