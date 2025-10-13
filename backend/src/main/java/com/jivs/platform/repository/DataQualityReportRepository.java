package com.jivs.platform.repository;

import com.jivs.platform.domain.quality.DataQualityCheck;
import com.jivs.platform.domain.quality.DataQualityReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for DataQualityReport entity
 * Maps to data_quality_results table - stores individual validation results
 */
@Repository
public interface DataQualityReportRepository extends JpaRepository<DataQualityReport, Long> {

    /**
     * Find all results for a specific quality check
     */
    List<DataQualityReport> findByCheck(DataQualityCheck check);

    /**
     * Find results by error type
     */
    List<DataQualityReport> findByErrorType(String errorType);

    /**
     * Find results by severity
     */
    List<DataQualityReport> findBySeverity(String severity);

    /**
     * Find unresolved quality issues
     */
    List<DataQualityReport> findByResolved(boolean resolved);

    /**
     * Find results by field name
     */
    List<DataQualityReport> findByFieldName(String fieldName);

    /**
     * Find results within date range
     */
    List<DataQualityReport> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find results resolved by specific user
     */
    List<DataQualityReport> findByResolvedBy(String resolvedBy);

    /**
     * Find results by record ID
     */
    List<DataQualityReport> findByRecordId(String recordId);
}
