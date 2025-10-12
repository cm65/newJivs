package com.jivs.platform.repository;

import com.jivs.platform.domain.businessobject.BusinessObject;
import com.jivs.platform.domain.businessobject.BusinessObjectDefinition;
import com.jivs.platform.domain.businessobject.BusinessObjectStatus;
import com.jivs.platform.domain.businessobject.BusinessObjectType;
import com.jivs.platform.domain.businessobject.LifecycleState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for BusinessObject entity
 */
@Repository
public interface BusinessObjectRepository extends JpaRepository<BusinessObject, Long> {

    /**
     * Find business object by external ID
     */
    Optional<BusinessObject> findByExternalId(String externalId);

    /**
     * Find business objects by definition
     */
    List<BusinessObject> findByDefinition(BusinessObjectDefinition definition);

    /**
     * Find business objects by current state
     */
    List<BusinessObject> findByCurrentState(LifecycleState state);

    /**
     * Find business objects by definition and state
     */
    List<BusinessObject> findByDefinitionAndCurrentState(BusinessObjectDefinition definition, LifecycleState state);

    /**
     * Find business objects by parent
     */
    List<BusinessObject> findByParent(BusinessObject parent);

    /**
     * Count business objects by parent
     */
    long countByParent(BusinessObject parent);

    /**
     * Search business objects by criteria
     */
    @Query("SELECT b FROM BusinessObject b WHERE " +
           "(:name IS NULL OR b.name LIKE %:name%) AND " +
           "(:type IS NULL OR b.type = :type) AND " +
           "(:category IS NULL OR b.category = :category) AND " +
           "(:status IS NULL OR b.status = :status) AND " +
           "(:parentId IS NULL OR b.parent.id = :parentId) AND " +
           "(:fromDate IS NULL OR b.createdDate >= :fromDate) AND " +
           "(:toDate IS NULL OR b.createdDate <= :toDate)")
    Page<BusinessObject> searchByCriteria(
        @Param("name") String name,
        @Param("type") BusinessObjectType type,
        @Param("category") String category,
        @Param("status") BusinessObjectStatus status,
        @Param("parentId") Long parentId,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate,
        Pageable pageable
    );

    /**
     * Count business objects by type
     */
    @Query("SELECT b.type, COUNT(b) FROM BusinessObject b GROUP BY b.type")
    Map<BusinessObjectType, Long> countByType();

    /**
     * Count business objects by status
     */
    @Query("SELECT b.status, COUNT(b) FROM BusinessObject b GROUP BY b.status")
    Map<BusinessObjectStatus, Long> countByStatus();

    /**
     * Count business objects by category
     */
    @Query("SELECT b.category, COUNT(b) FROM BusinessObject b GROUP BY b.category")
    Map<String, Long> countByCategory();

    /**
     * Find maximum hierarchy level
     */
    @Query("SELECT COALESCE(MAX(b.level), 0) FROM BusinessObject b")
    Integer findMaxLevel();

    /**
     * Find all descendants of a business object
     */
    @Query("SELECT b FROM BusinessObject b WHERE b.parent.id = :parentId OR b.parent.parent.id = :parentId OR " +
           "b.parent.parent.parent.id = :parentId OR b.parent.parent.parent.parent.id = :parentId")
    List<BusinessObject> findAllDescendants(@Param("parentId") Long parentId);

    /**
     * Find all parent business objects (objects that have children)
     */
    @Query("SELECT DISTINCT b FROM BusinessObject b WHERE EXISTS (SELECT 1 FROM BusinessObject c WHERE c.parent = b)")
    List<BusinessObject> findAllParents();

    /**
     * Find root business objects (objects with no parent)
     */
    List<BusinessObject> findByParentIsNull();
}
