package com.jivs.platform.repository;

import com.jivs.platform.domain.businessobject.BusinessObject;
import com.jivs.platform.domain.businessobject.BusinessObjectRelationship;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for BusinessObjectRelationship entities
 */
@Repository
public interface BusinessObjectRelationshipRepository extends JpaRepository<BusinessObjectRelationship, Long> {

    /**
     * Find relationships by source object
     */
    List<BusinessObjectRelationship> findBySource(BusinessObject source);

    /**
     * Find relationships by target object
     */
    List<BusinessObjectRelationship> findByTarget(BusinessObject target);

    /**
     * Find relationships by source object ID
     */
    List<BusinessObjectRelationship> findBySourceId(Long sourceId);

    /**
     * Find relationships by target object ID
     */
    List<BusinessObjectRelationship> findByTargetId(Long targetId);

    /**
     * Find active relationships by source
     */
    List<BusinessObjectRelationship> findBySourceAndActiveTrue(BusinessObject source);

    /**
     * Find active relationships by target
     */
    List<BusinessObjectRelationship> findByTargetAndActiveTrue(BusinessObject target);

    /**
     * Find relationship by source, target, and type
     */
    Optional<BusinessObjectRelationship> findBySourceAndTargetAndRelationshipType(
        BusinessObject source, BusinessObject target, String relationshipType);

    /**
     * Find relationships by type
     */
    List<BusinessObjectRelationship> findByRelationshipType(String relationshipType);

    /**
     * Find all relationships involving an object (as source or target)
     */
    @Query("SELECT r FROM BusinessObjectRelationship r WHERE r.source.id = :objectId OR r.target.id = :objectId")
    List<BusinessObjectRelationship> findAllByObjectId(@Param("objectId") Long objectId);

    /**
     * Find all active relationships involving an object
     */
    @Query("SELECT r FROM BusinessObjectRelationship r WHERE (r.source.id = :objectId OR r.target.id = :objectId) AND r.active = true")
    List<BusinessObjectRelationship> findAllActiveByObjectId(@Param("objectId") Long objectId);

    /**
     * Count relationships for an object
     */
    @Query("SELECT COUNT(r) FROM BusinessObjectRelationship r WHERE r.source.id = :objectId OR r.target.id = :objectId")
    long countByObjectId(@Param("objectId") Long objectId);

    /**
     * Count active relationships for an object
     */
    @Query("SELECT COUNT(r) FROM BusinessObjectRelationship r WHERE (r.source.id = :objectId OR r.target.id = :objectId) AND r.active = true")
    long countActiveByObjectId(@Param("objectId") Long objectId);

    /**
     * Count relationships by type
     */
    long countByRelationshipType(String relationshipType);

    /**
     * Count relationships by target object
     */
    long countByTarget(BusinessObject target);

    /**
     * Count relationships by type (grouped)
     */
    @Query("SELECT r.relationshipType, COUNT(r) FROM BusinessObjectRelationship r GROUP BY r.relationshipType")
    Map<String, Long> countByType();

    /**
     * Find relationships between two objects (bidirectional)
     */
    @Query("SELECT r FROM BusinessObjectRelationship r WHERE " +
           "(r.source.id = :objectId1 AND r.target.id = :objectId2) OR " +
           "(r.source.id = :objectId2 AND r.target.id = :objectId1)")
    List<BusinessObjectRelationship> findBetweenObjects(
        @Param("objectId1") Long objectId1,
        @Param("objectId2") Long objectId2);

    /**
     * Deactivate relationships by object ID
     */
    @Modifying
    @Query("UPDATE BusinessObjectRelationship r SET r.active = false WHERE r.source.id = :objectId OR r.target.id = :objectId")
    void deactivateByObjectId(@Param("objectId") Long objectId);

    /**
     * Delete relationships by object ID
     */
    @Modifying
    @Query("DELETE FROM BusinessObjectRelationship r WHERE r.source.id = :objectId OR r.target.id = :objectId")
    void deleteByObjectId(@Param("objectId") Long objectId);

    /**
     * Find paginated relationships
     */
    Page<BusinessObjectRelationship> findByActiveTrue(Pageable pageable);

    /**
     * Check if relationship exists between objects
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM BusinessObjectRelationship r WHERE " +
           "r.source.id = :sourceId AND r.target.id = :targetId AND r.relationshipType = :type")
    boolean existsBySourceIdAndTargetIdAndRelationshipType(
        @Param("sourceId") Long sourceId,
        @Param("targetId") Long targetId,
        @Param("type") String type);
}
