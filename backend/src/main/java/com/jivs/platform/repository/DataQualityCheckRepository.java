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
     * Find checks by rule
     */
    List<DataQualityCheck> findByRule(DataQualityRule rule);

    /**
     * Find checks by status
     */
    List<DataQualityCheck> findByCheckStatus(String checkStatus);

    /**
     * Find checks within start time range
     */
    List<DataQualityCheck> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find checks within end time range
     */
    List<DataQualityCheck> findByEndTimeBetween(LocalDateTime start, LocalDateTime end);
}
