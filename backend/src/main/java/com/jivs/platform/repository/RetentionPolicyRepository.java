package com.jivs.platform.repository;

import com.jivs.platform.domain.retention.RetentionAction;
import com.jivs.platform.domain.retention.RetentionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for RetentionPolicy entity
 */
@Repository
public interface RetentionPolicyRepository extends JpaRepository<RetentionPolicy, Long> {

    /**
     * Find policy by name
     */
    Optional<RetentionPolicy> findByName(String name);

    /**
     * Find active policies
     */
    List<RetentionPolicy> findByActive(boolean active);

    /**
     * Find policies by dataset type
     */
    List<RetentionPolicy> findByDatasetType(String datasetType);

    /**
     * Find active policies by dataset type, ordered by priority
     */
    List<RetentionPolicy> findByDatasetTypeAndActiveOrderByPriorityDesc(String datasetType, boolean active);

    /**
     * Find policies by action
     */
    List<RetentionPolicy> findByAction(RetentionAction action);

    /**
     * Find policies by entity type and active status
     */
    List<RetentionPolicy> findByEntityTypeAndActive(String entityType, boolean active);

    /**
     * Count policies by active status
     */
    long countByActive(boolean active);
}
