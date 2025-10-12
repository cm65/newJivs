package com.jivs.platform.repository;

import com.jivs.platform.domain.quality.DataQualityRule;
import com.jivs.platform.domain.quality.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for DataQualityRule entity
 */
@Repository
public interface DataQualityRuleRepository extends JpaRepository<DataQualityRule, Long> {

    /**
     * Find active rules by dataset type
     */
    List<DataQualityRule> findByDatasetTypeAndActive(String datasetType, boolean active);

    /**
     * Find rules by type
     */
    List<DataQualityRule> findByRuleType(RuleType ruleType);

    /**
     * Find all active rules
     */
    List<DataQualityRule> findByActive(boolean active);

    /**
     * Find rules by dataset type
     */
    List<DataQualityRule> findByDatasetType(String datasetType);
}
