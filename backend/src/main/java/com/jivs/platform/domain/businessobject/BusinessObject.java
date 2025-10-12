package com.jivs.platform.domain.businessobject;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity representing a business object instance
 */
@Entity
@Table(name = "business_objects")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class BusinessObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinessObjectType type;

    @Column(nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinessObjectStatus status = BusinessObjectStatus.DRAFT;

    @Column(unique = true)
    private String identifier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "definition_id")
    private BusinessObjectDefinition definition;

    @Column(nullable = false, unique = true)
    private String externalId;

    @ElementCollection
    @CollectionTable(name = "business_object_data", joinColumns = @JoinColumn(name = "object_id"))
    @MapKeyColumn(name = "field_name")
    @Column(name = "field_value", columnDefinition = "TEXT")
    private Map<String, String> data = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "business_object_attributes", joinColumns = @JoinColumn(name = "object_id"))
    @MapKeyColumn(name = "attr_name")
    @Column(name = "attr_value", columnDefinition = "TEXT")
    private Map<String, Object> attributes = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "business_object_metadata", joinColumns = @JoinColumn(name = "object_id"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value", columnDefinition = "TEXT")
    private Map<String, Object> metadata = new HashMap<>();

    @OneToMany(mappedBy = "businessObject", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BusinessObjectLifecycle> lifecycles = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LifecycleState currentState = LifecycleState.DRAFT;

    @Column(nullable = false)
    private Integer version = 1;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime modifiedDate;

    private LocalDateTime archivedDate;

    private String createdBy;

    private String updatedBy;

    private String modifiedBy;

    private Long archivedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private BusinessObject parent;

    @Column(name = "hierarchy_level")
    private Integer level = 0;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BusinessObjectDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(BusinessObjectDefinition definition) {
        this.definition = definition;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    public List<BusinessObjectLifecycle> getLifecycles() {
        return lifecycles;
    }

    public void setLifecycles(List<BusinessObjectLifecycle> lifecycles) {
        this.lifecycles = lifecycles;
    }

    public LifecycleState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(LifecycleState currentState) {
        this.currentState = currentState;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public BusinessObject getParent() {
        return parent;
    }

    public void setParent(BusinessObject parent) {
        this.parent = parent;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BusinessObjectType getType() {
        return type;
    }

    public void setType(BusinessObjectType type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BusinessObjectStatus getStatus() {
        return status;
    }

    public void setStatus(BusinessObjectStatus status) {
        this.status = status;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(LocalDateTime modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public LocalDateTime getArchivedDate() {
        return archivedDate;
    }

    public void setArchivedDate(LocalDateTime archivedDate) {
        this.archivedDate = archivedDate;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public Long getArchivedBy() {
        return archivedBy;
    }

    public void setArchivedBy(Long archivedBy) {
        this.archivedBy = archivedBy;
    }
}
