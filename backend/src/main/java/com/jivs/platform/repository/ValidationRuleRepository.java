package com.jivs.platform.repository;

import com.jivs.platform.domain.migration.ValidationRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ValidationRule entity
 */
@Repository
public interface ValidationRuleRepository extends JpaRepository<ValidationRule, Long> {

    /**
     * Find validation rules by rule type
     */
    List<ValidationRule> findByRuleType(String ruleType);

    /**
     * Find active validation rules
     */
    List<ValidationRule> findByActiveTrue();

    /**
     * Find validation rules by source system
     */
    List<ValidationRule> findBySourceSystem(String sourceSystem);

    /**
     * Find validation rules by target system
     */
    List<ValidationRule> findByTargetSystem(String targetSystem);

    /**
     * Find validation rules by source and target systems
     */
    List<ValidationRule> findBySourceSystemAndTargetSystem(String sourceSystem, String targetSystem);

    /**
     * Find active validation rules by source and target systems
     */
    @Query("SELECT v FROM ValidationRule v WHERE v.sourceSystem = :sourceSystem " +
           "AND v.targetSystem = :targetSystem AND v.active = true " +
           "ORDER BY v.executionOrder ASC")
    List<ValidationRule> findActiveRulesBySystemsOrderedByExecution(
            @Param("sourceSystem") String sourceSystem,
            @Param("targetSystem") String targetSystem
    );

    /**
     * Find critical validation rules
     */
    List<ValidationRule> findByCriticalTrue();

    /**
     * Find validation rule by name
     */
    Optional<ValidationRule> findByName(String name);

    /**
     * Check if validation rule name exists
     */
    boolean existsByName(String name);

    /**
     * Find validation rules with pagination
     */
    Page<ValidationRule> findByActive(boolean active, Pageable pageable);

    /**
     * Count active validation rules
     */
    long countByActiveTrue();

    /**
     * Find validation rules by rule type and active status
     */
    List<ValidationRule> findByRuleTypeAndActive(String ruleType, boolean active);
}
