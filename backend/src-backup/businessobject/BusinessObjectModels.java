package com.jivs.platform.service.businessobject;

import com.jivs.platform.domain.businessobject.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Business object request
 */
class BusinessObjectRequest {
    private String name;
    private String description;
    private BusinessObjectType type;
    private String category;
    private Long parentId;
    private Long userId;
    private Map<String, Object> metadata;
    private Map<String, Object> attributes;

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BusinessObjectType getType() { return type; }
    public void setType(BusinessObjectType type) { this.type = type; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
}

/**
 * Business object update request
 */
class BusinessObjectUpdateRequest {
    private String name;
    private String description;
    private String category;
    private Long userId;
    private Map<String, Object> metadata;
    private Map<String, Object> attributes;

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
}

/**
 * Business object search criteria
 */
class BusinessObjectSearchCriteria {
    private String name;
    private BusinessObjectType type;
    private String category;
    private BusinessObjectStatus status;
    private Long parentId;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;

    public boolean isEmpty() {
        return name == null && type == null && category == null &&
               status == null && parentId == null &&
               fromDate == null && toDate == null;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BusinessObjectType getType() { return type; }
    public void setType(BusinessObjectType type) { this.type = type; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BusinessObjectStatus getStatus() { return status; }
    public void setStatus(BusinessObjectStatus status) { this.status = status; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public LocalDateTime getFromDate() { return fromDate; }
    public void setFromDate(LocalDateTime fromDate) { this.fromDate = fromDate; }
    public LocalDateTime getToDate() { return toDate; }
    public void setToDate(LocalDateTime toDate) { this.toDate = toDate; }
}

/**
 * Relationship creation request
 */
class RelationshipRequest {
    private Long sourceId;
    private Long targetId;
    private String relationshipType;
    private Long userId;
    private Map<String, Object> attributes;

    // Getters and setters
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getRelationshipType() { return relationshipType; }
    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
}

/**
 * Business object lineage
 */
class BusinessObjectLineage {
    private BusinessObject businessObject;
    private List<BusinessObject> ancestors;
    private List<BusinessObject> descendants;
    private List<BusinessObjectRelationship> relationships;

    // Getters and setters
    public BusinessObject getBusinessObject() { return businessObject; }
    public void setBusinessObject(BusinessObject businessObject) {
        this.businessObject = businessObject;
    }
    public List<BusinessObject> getAncestors() { return ancestors; }
    public void setAncestors(List<BusinessObject> ancestors) { this.ancestors = ancestors; }
    public List<BusinessObject> getDescendants() { return descendants; }
    public void setDescendants(List<BusinessObject> descendants) { this.descendants = descendants; }
    public List<BusinessObjectRelationship> getRelationships() { return relationships; }
    public void setRelationships(List<BusinessObjectRelationship> relationships) {
        this.relationships = relationships;
    }
}

/**
 * Clone request
 */
class CloneRequest {
    private String newName;
    private Long userId;
    private boolean includeMetadata = true;
    private boolean includeAttributes = true;
    private boolean includeRelationships = false;

    // Getters and setters
    public String getNewName() { return newName; }
    public void setNewName(String newName) { this.newName = newName; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public boolean isIncludeMetadata() { return includeMetadata; }
    public void setIncludeMetadata(boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
    }
    public boolean isIncludeAttributes() { return includeAttributes; }
    public void setIncludeAttributes(boolean includeAttributes) {
        this.includeAttributes = includeAttributes;
    }
    public boolean isIncludeRelationships() { return includeRelationships; }
    public void setIncludeRelationships(boolean includeRelationships) {
        this.includeRelationships = includeRelationships;
    }
}

/**
 * Business object statistics
 */
class BusinessObjectStatistics {
    private long totalCount;
    private Map<BusinessObjectType, Long> countByType;
    private Map<BusinessObjectStatus, Long> countByStatus;
    private Map<String, Long> countByCategory;
    private long totalRelationships;
    private Map<String, Long> relationshipsByType;
    private int maxDepth;
    private double averageChildrenPerParent;

    // Getters and setters
    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
    public Map<BusinessObjectType, Long> getCountByType() { return countByType; }
    public void setCountByType(Map<BusinessObjectType, Long> countByType) {
        this.countByType = countByType;
    }
    public Map<BusinessObjectStatus, Long> getCountByStatus() { return countByStatus; }
    public void setCountByStatus(Map<BusinessObjectStatus, Long> countByStatus) {
        this.countByStatus = countByStatus;
    }
    public Map<String, Long> getCountByCategory() { return countByCategory; }
    public void setCountByCategory(Map<String, Long> countByCategory) {
        this.countByCategory = countByCategory;
    }
    public long getTotalRelationships() { return totalRelationships; }
    public void setTotalRelationships(long totalRelationships) {
        this.totalRelationships = totalRelationships;
    }
    public Map<String, Long> getRelationshipsByType() { return relationshipsByType; }
    public void setRelationshipsByType(Map<String, Long> relationshipsByType) {
        this.relationshipsByType = relationshipsByType;
    }
    public int getMaxDepth() { return maxDepth; }
    public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }
    public double getAverageChildrenPerParent() { return averageChildrenPerParent; }
    public void setAverageChildrenPerParent(double averageChildrenPerParent) {
        this.averageChildrenPerParent = averageChildrenPerParent;
    }
}

/**
 * Export request
 */
class ExportRequest {
    private List<Long> objectIds;
    private String format = "JSON";
    private boolean includeRelationships = true;
    private boolean includeMetadata = true;
    private boolean includeHierarchy = false;

    // Getters and setters
    public List<Long> getObjectIds() { return objectIds; }
    public void setObjectIds(List<Long> objectIds) { this.objectIds = objectIds; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public boolean isIncludeRelationships() { return includeRelationships; }
    public void setIncludeRelationships(boolean includeRelationships) {
        this.includeRelationships = includeRelationships;
    }
    public boolean isIncludeMetadata() { return includeMetadata; }
    public void setIncludeMetadata(boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
    }
    public boolean isIncludeHierarchy() { return includeHierarchy; }
    public void setIncludeHierarchy(boolean includeHierarchy) {
        this.includeHierarchy = includeHierarchy;
    }
}

/**
 * Business object export
 */
class BusinessObjectExport {
    private LocalDateTime exportDate;
    private String format;
    private List<BusinessObject> objects;
    private Map<Long, List<BusinessObjectRelationship>> relationships;
    private Map<Long, Object> metadata;

    // Getters and setters
    public LocalDateTime getExportDate() { return exportDate; }
    public void setExportDate(LocalDateTime exportDate) { this.exportDate = exportDate; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public List<BusinessObject> getObjects() { return objects; }
    public void setObjects(List<BusinessObject> objects) { this.objects = objects; }
    public Map<Long, List<BusinessObjectRelationship>> getRelationships() {
        return relationships;
    }
    public void setRelationships(Map<Long, List<BusinessObjectRelationship>> relationships) {
        this.relationships = relationships;
    }
    public Map<Long, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<Long, Object> metadata) { this.metadata = metadata; }
}

/**
 * Business object import
 */
class BusinessObjectImport {
    private List<BusinessObject> objects;
    private Map<Long, List<BusinessObjectRelationship>> relationships;
    private Map<Long, Object> metadata;

    // Getters and setters
    public List<BusinessObject> getObjects() { return objects; }
    public void setObjects(List<BusinessObject> objects) { this.objects = objects; }
    public Map<Long, List<BusinessObjectRelationship>> getRelationships() {
        return relationships;
    }
    public void setRelationships(Map<Long, List<BusinessObjectRelationship>> relationships) {
        this.relationships = relationships;
    }
    public Map<Long, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<Long, Object> metadata) { this.metadata = metadata; }
}

/**
 * Import result
 */
class ImportResult {
    private int totalCount;
    private int successCount;
    private int errorCount;
    private List<String> errors = new ArrayList<>();

    public void incrementSuccessCount() { successCount++; }
    public void addError(String error) {
        errors.add(error);
        errorCount++;
    }

    // Getters and setters
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}