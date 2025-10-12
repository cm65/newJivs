package com.jivs.platform.repository;

import com.jivs.platform.domain.quality.DataQualityReport;
import com.jivs.platform.domain.quality.QualityCheckStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for DataQualityReport entity
 */
@Repository
public interface DataQualityReportRepository extends JpaRepository<DataQualityReport, Long> {

    /**
     * Find reports by dataset
     */
    List<DataQualityReport> findByDatasetId(Long datasetId);

    /**
     * Find reports by dataset type
     */
    List<DataQualityReport> findByDatasetType(String datasetType);

    /**
     * Find reports by status
     */
    List<DataQualityReport> findByStatus(QualityCheckStatus status);

    /**
     * Find latest report for dataset
     */
    Optional<DataQualityReport> findFirstByDatasetIdOrderByCheckDateDesc(Long datasetId);

    /**
     * Find reports within date range
     */
    List<DataQualityReport> findByCheckDateBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find reports by quality score range
     */
    List<DataQualityReport> findByQualityScoreBetween(Double minScore, Double maxScore);
}
