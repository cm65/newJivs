package com.jivs.platform.repository;

import com.jivs.platform.domain.quality.DataQualityCheck;
import com.jivs.platform.domain.quality.DataQualityReport;
import com.jivs.platform.domain.quality.DataQualityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for DataQualityCheck entity
 */
@Repository
public interface DataQualityCheckRepository extends JpaRepository<DataQualityCheck, Long> {

    /**
     * Find checks by report
     */
    List<DataQualityCheck> findByReport(DataQualityReport report);

    /**
     * Find checks by rule
     */
    List<DataQualityCheck> findByRule(DataQualityRule rule);

    /**
     * Find failed checks
     */
    List<DataQualityCheck> findByPassed(boolean passed);

    /**
     * Find checks within date range
     */
    List<DataQualityCheck> findByExecutionTimeBetween(LocalDateTime start, LocalDateTime end);
}
