package com.jivs.platform.repository;

import com.jivs.platform.domain.transformation.TransformationRule;
import com.jivs.platform.domain.transformation.TransformationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TransformationRule entity
 */
@Repository
public interface TransformationRuleRepository extends JpaRepository<TransformationRule, Long> {

    /**
     * Find rules by migration ID
     */
    List<TransformationRule> findByMigrationId(Long migrationId);

    /**
     * Find rules by migration ID and active status, ordered by execution order
     */
    List<TransformationRule> findByMigrationIdAndActiveOrderByExecutionOrder(Long migrationId, boolean active);

    /**
     * Find rules by type
     */
    List<TransformationRule> findByType(TransformationType type);

    /**
     * Find all active rules
     */
    List<TransformationRule> findByActive(boolean active);

    /**
     * Find rules by source system, target system and active status
     */
    List<TransformationRule> findBySourceSystemAndTargetSystemAndActive(
            String sourceSystem,
            String targetSystem,
            boolean active
    );
}
