package com.jivs.platform.service.businessobject;

import com.jivs.platform.domain.businessobject.BusinessObject;
import com.jivs.platform.domain.businessobject.BusinessObjectType;
import com.jivs.platform.domain.businessobject.BusinessObjectStatus;
import com.jivs.platform.domain.businessobject.BusinessObjectRelationship;
import com.jivs.platform.repository.BusinessObjectRepository;
import com.jivs.platform.repository.BusinessObjectRelationshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing business objects and their relationships
 */
@Service
@RequiredArgsConstructor
public class BusinessObjectService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BusinessObjectService.class);

    private final BusinessObjectRepository businessObjectRepository;
    private final BusinessObjectRelationshipRepository relationshipRepository;
    private final MetadataService metadataService;
    private final HierarchyService hierarchyService;
    private final LifecycleService lifecycleService;

    /**
     * Create a new business object
     */
    @Transactional
    public BusinessObject createBusinessObject(BusinessObjectRequest request) {
        log.info("Creating business object: {}", request.getName());

        // Validate request
        validateBusinessObjectRequest(request);

        BusinessObject businessObject = new BusinessObject();
        businessObject.setName(request.getName());
        businessObject.setDescription(request.getDescription());
        businessObject.setType(request.getType());
        businessObject.setCategory(request.getCategory());
        businessObject.setStatus(BusinessObjectStatus.DRAFT);
        businessObject.setCreatedDate(LocalDateTime.now());
        businessObject.setCreatedBy(request.getUserId() != null ? request.getUserId().toString() : null);

        // Set parent if specified
        if (request.getParentId() != null) {
            BusinessObject parent = businessObjectRepository.findById(request.getParentId())
                .orElseThrow(() -> new IllegalArgumentException("Parent business object not found"));
            businessObject.setParent(parent);
            businessObject.setLevel(parent.getLevel() + 1);
        } else {
            businessObject.setLevel(0);
        }

        // Set metadata
        if (request.getMetadata() != null) {
            businessObject.setMetadata(metadataService.createMetadata(request.getMetadata()));
        }

        // Set attributes
        businessObject.setAttributes(request.getAttributes());

        // Calculate unique identifier
        businessObject.setIdentifier(generateUniqueIdentifier(businessObject));

        BusinessObject savedObject = businessObjectRepository.save(businessObject);

        // Create initial lifecycle record
        lifecycleService.initializeLifecycle(savedObject);

        log.info("Business object created with ID: {}", savedObject.getId());
        return savedObject;
    }

    /**
     * Update business object
     */
    @Transactional
    @CacheEvict(value = "businessObjects", key = "#id")
    public BusinessObject updateBusinessObject(Long id, BusinessObjectUpdateRequest request) {
        log.info("Updating business object: {}", id);

        BusinessObject businessObject = businessObjectRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Business object not found: " + id));

        // Update basic fields
        if (request.getName() != null) {
            businessObject.setName(request.getName());
        }
        if (request.getDescription() != null) {
            businessObject.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            businessObject.setCategory(request.getCategory());
        }

        // Update metadata
        if (request.getMetadata() != null) {
            businessObject.setMetadata(metadataService.updateMetadata(
                businessObject.getMetadata(),
                request.getMetadata()
            ));
        }

        // Update attributes
        if (request.getAttributes() != null) {
            businessObject.setAttributes(mergeAttributes(
                businessObject.getAttributes(),
                request.getAttributes()
            ));
        }

        businessObject.setModifiedDate(LocalDateTime.now());
        businessObject.setModifiedBy(request.getUserId() != null ? request.getUserId().toString() : null);

        // Update lifecycle
        lifecycleService.recordUpdate(businessObject);

        return businessObjectRepository.save(businessObject);
    }

    /**
     * Get business object by ID
     */
    @Cacheable(value = "businessObjects", key = "#id")
    public BusinessObject getBusinessObject(Long id) {
        return businessObjectRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Business object not found: " + id));
    }

    /**
     * Search business objects
     */
    public Page<BusinessObject> searchBusinessObjects(BusinessObjectSearchCriteria criteria, Pageable pageable) {
        log.debug("Searching business objects with criteria: {}", criteria);

        if (criteria.isEmpty()) {
            return businessObjectRepository.findAll(pageable);
        }

        return businessObjectRepository.searchByCriteria(
            criteria.getName(),
            criteria.getType(),
            criteria.getCategory(),
            criteria.getStatus(),
            criteria.getParentId(),
            criteria.getFromDate(),
            criteria.getToDate(),
            pageable
        );
    }

    /**
     * Create relationship between business objects
     */
    @Transactional
    public BusinessObjectRelationship createRelationship(RelationshipRequest request) {
        log.info("Creating relationship between {} and {}",
            request.getSourceId(), request.getTargetId());

        // Validate objects exist
        BusinessObject source = getBusinessObject(request.getSourceId());
        BusinessObject target = getBusinessObject(request.getTargetId());

        // Check for circular dependencies
        if (wouldCreateCircularDependency(source, target, request.getRelationshipType())) {
            throw new IllegalStateException("Cannot create circular dependency");
        }

        // Check if relationship already exists
        Optional<BusinessObjectRelationship> existing = relationshipRepository
            .findBySourceAndTargetAndRelationshipType(source, target, request.getRelationshipType());

        if (existing.isPresent()) {
            throw new IllegalStateException("Relationship already exists");
        }

        BusinessObjectRelationship relationship = new BusinessObjectRelationship();
        relationship.setSource(source);
        relationship.setTarget(target);
        relationship.setRelationshipType(request.getRelationshipType());
        relationship.setAttributes(request.getAttributes());
        relationship.setCreatedDate(LocalDateTime.now());
        relationship.setCreatedBy(request.getUserId() != null ? request.getUserId().toString() : null);

        return relationshipRepository.save(relationship);
    }

    /**
     * Get relationships for a business object
     */
    public List<BusinessObjectRelationship> getRelationships(Long businessObjectId) {
        BusinessObject businessObject = getBusinessObject(businessObjectId);

        List<BusinessObjectRelationship> relationships = new ArrayList<>();
        relationships.addAll(relationshipRepository.findBySource(businessObject));
        relationships.addAll(relationshipRepository.findByTarget(businessObject));

        return relationships;
    }

    /**
     * Get business object hierarchy
     */
    public BusinessObjectHierarchy getHierarchy(Long rootId) {
        log.debug("Getting hierarchy for business object: {}", rootId);

        BusinessObject root = getBusinessObject(rootId);
        return hierarchyService.buildHierarchy(root);
    }

    /**
     * Get business object lineage
     */
    public BusinessObjectLineage getLineage(Long businessObjectId) {
        log.debug("Getting lineage for business object: {}", businessObjectId);

        BusinessObject businessObject = getBusinessObject(businessObjectId);
        return buildLineage(businessObject);
    }

    /**
     * Clone business object
     */
    @Transactional
    public BusinessObject cloneBusinessObject(Long id, CloneRequest request) {
        log.info("Cloning business object: {}", id);

        BusinessObject original = getBusinessObject(id);

        BusinessObject clone = new BusinessObject();
        clone.setName(request.getNewName() != null ? request.getNewName() : original.getName() + " (Copy)");
        clone.setDescription(original.getDescription());
        clone.setType(original.getType());
        clone.setCategory(original.getCategory());
        clone.setStatus(BusinessObjectStatus.DRAFT);
        clone.setParent(original.getParent());
        clone.setLevel(original.getLevel());
        clone.setCreatedDate(LocalDateTime.now());
        clone.setCreatedBy(request.getUserId() != null ? request.getUserId().toString() : null);

        // Clone metadata if requested
        if (request.isIncludeMetadata() && original.getMetadata() != null) {
            clone.setMetadata(metadataService.cloneMetadata(original.getMetadata()));
        }

        // Clone attributes if requested
        if (request.isIncludeAttributes() && original.getAttributes() != null) {
            clone.setAttributes(new HashMap<>(original.getAttributes()));
        }

        clone.setIdentifier(generateUniqueIdentifier(clone));

        BusinessObject savedClone = businessObjectRepository.save(clone);

        // Clone relationships if requested
        if (request.isIncludeRelationships()) {
            cloneRelationships(original, savedClone, request.getUserId());
        }

        log.info("Business object cloned with new ID: {}", savedClone.getId());
        return savedClone;
    }

    /**
     * Archive business object
     */
    @Transactional
    public void archiveBusinessObject(Long id, Long userId) {
        log.info("Archiving business object: {}", id);

        BusinessObject businessObject = getBusinessObject(id);

        // Check if can be archived
        if (!canBeArchived(businessObject)) {
            throw new IllegalStateException("Business object cannot be archived due to active dependencies");
        }

        businessObject.setStatus(BusinessObjectStatus.ARCHIVED);
        businessObject.setArchivedDate(LocalDateTime.now());
        businessObject.setArchivedBy(userId);

        businessObjectRepository.save(businessObject);

        // Archive child objects if any
        archiveChildObjects(businessObject, userId);

        // Record in lifecycle
        lifecycleService.recordArchival(businessObject);

        log.info("Business object archived: {}", id);
    }

    /**
     * Delete business object
     */
    @Transactional
    @CacheEvict(value = "businessObjects", key = "#id")
    public void deleteBusinessObject(Long id, boolean cascade) {
        log.info("Deleting business object: {} (cascade: {})", id, cascade);

        BusinessObject businessObject = getBusinessObject(id);

        // Check if can be deleted
        if (!canBeDeleted(businessObject)) {
            throw new IllegalStateException("Business object cannot be deleted due to active dependencies");
        }

        if (cascade) {
            // Delete relationships
            deleteRelationships(businessObject);

            // Delete child objects
            deleteChildObjects(businessObject);
        }

        // Record deletion in lifecycle before actual deletion
        lifecycleService.recordDeletion(businessObject);

        businessObjectRepository.delete(businessObject);

        log.info("Business object deleted: {}", id);
    }

    /**
     * Get business object statistics
     */
    public BusinessObjectStatistics getStatistics() {
        BusinessObjectStatistics stats = new BusinessObjectStatistics();

        stats.setTotalCount(businessObjectRepository.count());
        stats.setCountByType(businessObjectRepository.countByType());
        stats.setCountByStatus(businessObjectRepository.countByStatus());
        stats.setCountByCategory(businessObjectRepository.countByCategory());

        // Get relationship statistics
        stats.setTotalRelationships(relationshipRepository.count());
        stats.setRelationshipsByType(relationshipRepository.countByType());

        // Get hierarchy statistics
        stats.setMaxDepth(businessObjectRepository.findMaxLevel());
        stats.setAverageChildrenPerParent(calculateAverageChildren());

        return stats;
    }

    /**
     * Export business objects
     */
    public BusinessObjectExport exportBusinessObjects(ExportRequest request) {
        log.info("Exporting business objects with criteria: {}", request);

        List<BusinessObject> objects;

        if (request.getObjectIds() != null && !request.getObjectIds().isEmpty()) {
            objects = businessObjectRepository.findAllById(request.getObjectIds());
        } else {
            objects = businessObjectRepository.findAll();
        }

        BusinessObjectExport export = new BusinessObjectExport();
        export.setExportDate(LocalDateTime.now());
        export.setFormat(request.getFormat());
        export.setObjects(objects);

        if (request.isIncludeRelationships()) {
            Map<Long, List<BusinessObjectRelationship>> relationshipsMap = new HashMap<>();
            for (BusinessObject obj : objects) {
                relationshipsMap.put(obj.getId(), getRelationships(obj.getId()));
            }
            export.setRelationships(relationshipsMap);
        }

        if (request.isIncludeMetadata()) {
            Map<Long, Object> metadataMap = new HashMap<>();
            for (BusinessObject obj : objects) {
                if (obj.getMetadata() != null) {
                    metadataMap.put(obj.getId(), obj.getMetadata());
                }
            }
            export.setMetadata(metadataMap);
        }

        return export;
    }

    /**
     * Import business objects
     */
    @Transactional
    public ImportResult importBusinessObjects(BusinessObjectImport importData) {
        log.info("Importing {} business objects", importData.getObjects().size());

        ImportResult result = new ImportResult();
        Map<Long, Long> idMapping = new HashMap<>(); // Old ID to new ID mapping

        // Import objects
        for (BusinessObject obj : importData.getObjects()) {
            try {
                Long oldId = obj.getId();
                obj.setId(null); // Clear ID for new creation
                obj.setCreatedDate(LocalDateTime.now());
                obj.setStatus(BusinessObjectStatus.DRAFT);

                BusinessObject saved = businessObjectRepository.save(obj);
                idMapping.put(oldId, saved.getId());
                result.incrementSuccessCount();

            } catch (Exception e) {
                log.error("Failed to import business object: {}", obj.getName(), e);
                result.addError("Failed to import: " + obj.getName() + " - " + e.getMessage());
            }
        }

        // Import relationships with ID mapping
        if (importData.getRelationships() != null) {
            importRelationships(importData.getRelationships(), idMapping, result);
        }

        result.setTotalCount(importData.getObjects().size());
        log.info("Import completed. Success: {}, Failed: {}",
            result.getSuccessCount(), result.getErrorCount());

        return result;
    }

    // Helper methods
    private void validateBusinessObjectRequest(BusinessObjectRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Business object name is required");
        }
        if (request.getType() == null) {
            throw new IllegalArgumentException("Business object type is required");
        }
    }

    private String generateUniqueIdentifier(BusinessObject businessObject) {
        return String.format("%s_%s_%s_%d",
            businessObject.getType().toString().toLowerCase(),
            businessObject.getCategory().toLowerCase().replaceAll("\\s+", "_"),
            businessObject.getName().toLowerCase().replaceAll("\\s+", "_"),
            System.currentTimeMillis()
        );
    }

    private Map<String, Object> mergeAttributes(
            Map<String, Object> existing,
            Map<String, Object> updates) {

        if (existing == null) {
            return updates;
        }

        Map<String, Object> merged = new HashMap<>(existing);
        merged.putAll(updates);
        return merged;
    }

    private boolean wouldCreateCircularDependency(
            BusinessObject source,
            BusinessObject target,
            String relationshipType) {

        // Check if target is already an ancestor of source
        if ("PARENT_CHILD".equals(relationshipType) || "DEPENDS_ON".equals(relationshipType)) {
            return isAncestor(target, source);
        }
        return false;
    }

    private boolean isAncestor(BusinessObject potentialAncestor, BusinessObject object) {
        if (object.getParent() == null) {
            return false;
        }
        if (object.getParent().getId().equals(potentialAncestor.getId())) {
            return true;
        }
        return isAncestor(potentialAncestor, object.getParent());
    }

    private BusinessObjectLineage buildLineage(BusinessObject businessObject) {
        BusinessObjectLineage lineage = new BusinessObjectLineage();
        lineage.setBusinessObject(businessObject);

        // Build ancestor chain
        List<BusinessObject> ancestors = new ArrayList<>();
        BusinessObject current = businessObject.getParent();
        while (current != null) {
            ancestors.add(current);
            current = current.getParent();
        }
        lineage.setAncestors(ancestors);

        // Build descendant tree
        List<BusinessObject> descendants = businessObjectRepository.findAllDescendants(businessObject.getId());
        lineage.setDescendants(descendants);

        // Get related objects
        List<BusinessObjectRelationship> relationships = getRelationships(businessObject.getId());
        lineage.setRelationships(relationships);

        return lineage;
    }

    private void cloneRelationships(BusinessObject original, BusinessObject clone, Long userId) {
        List<BusinessObjectRelationship> relationships = getRelationships(original.getId());

        for (BusinessObjectRelationship rel : relationships) {
            BusinessObjectRelationship clonedRel = new BusinessObjectRelationship();

            if (rel.getSource().getId().equals(original.getId())) {
                clonedRel.setSource(clone);
                clonedRel.setTarget(rel.getTarget());
            } else {
                clonedRel.setSource(rel.getSource());
                clonedRel.setTarget(clone);
            }

            clonedRel.setRelationshipType(rel.getRelationshipType());
            clonedRel.setAttributes(rel.getAttributes() != null ? new HashMap<>(rel.getAttributes()) : null);
            clonedRel.setCreatedDate(LocalDateTime.now());
            clonedRel.setCreatedBy(userId != null ? userId.toString() : null);

            relationshipRepository.save(clonedRel);
        }
    }

    private boolean canBeArchived(BusinessObject businessObject) {
        // Check if there are active dependencies
        List<BusinessObject> children = businessObjectRepository.findByParent(businessObject);
        return children.stream()
            .allMatch(child -> child.getStatus() == BusinessObjectStatus.ARCHIVED ||
                             child.getStatus() == BusinessObjectStatus.DELETED);
    }

    private boolean canBeDeleted(BusinessObject businessObject) {
        // Check if there are any dependencies
        long dependentCount = relationshipRepository.countByTarget(businessObject);
        return dependentCount == 0 || businessObject.getStatus() == BusinessObjectStatus.ARCHIVED;
    }

    private void archiveChildObjects(BusinessObject parent, Long userId) {
        List<BusinessObject> children = businessObjectRepository.findByParent(parent);
        for (BusinessObject child : children) {
            archiveBusinessObject(child.getId(), userId);
        }
    }

    private void deleteRelationships(BusinessObject businessObject) {
        List<BusinessObjectRelationship> relationships = getRelationships(businessObject.getId());
        relationshipRepository.deleteAll(relationships);
    }

    private void deleteChildObjects(BusinessObject parent) {
        List<BusinessObject> children = businessObjectRepository.findByParent(parent);
        for (BusinessObject child : children) {
            deleteBusinessObject(child.getId(), true);
        }
    }

    private double calculateAverageChildren() {
        List<BusinessObject> parents = businessObjectRepository.findAllParents();
        if (parents.isEmpty()) {
            return 0;
        }

        int totalChildren = 0;
        for (BusinessObject parent : parents) {
            totalChildren += businessObjectRepository.countByParent(parent);
        }

        return (double) totalChildren / parents.size();
    }

    private void importRelationships(
            Map<Long, List<BusinessObjectRelationship>> relationships,
            Map<Long, Long> idMapping,
            ImportResult result) {

        for (Map.Entry<Long, List<BusinessObjectRelationship>> entry : relationships.entrySet()) {
            Long newSourceId = idMapping.get(entry.getKey());
            if (newSourceId == null) continue;

            for (BusinessObjectRelationship rel : entry.getValue()) {
                try {
                    Long newTargetId = idMapping.get(rel.getTarget().getId());
                    if (newTargetId == null) continue;

                    BusinessObjectRelationship newRel = new BusinessObjectRelationship();
                    newRel.setSource(businessObjectRepository.findById(newSourceId).orElse(null));
                    newRel.setTarget(businessObjectRepository.findById(newTargetId).orElse(null));
                    newRel.setRelationshipType(rel.getRelationshipType());
                    newRel.setAttributes(rel.getAttributes());
                    newRel.setCreatedDate(LocalDateTime.now());
                    newRel.setCreatedBy("system");

                    relationshipRepository.save(newRel);

                } catch (Exception e) {
                    log.error("Failed to import relationship", e);
                    result.addError("Failed to import relationship: " + e.getMessage());
                }
            }
        }
    }
}