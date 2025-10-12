package com.jivs.platform.domain.businessobject;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing relationships between business objects
 */
@Entity
@Table(name = "business_object_relationships", indexes = {
    @Index(name = "idx_source_id", columnList = "source_id"),
    @Index(name = "idx_target_id", columnList = "target_id"),
    @Index(name = "idx_relationship_type", columnList = "relationship_type"),
    @Index(name = "idx_source_target", columnList = "source_id, target_id")
})
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class BusinessObjectRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private BusinessObject source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private BusinessObject target;

    @Column(name = "relationship_type", nullable = false, length = 100)
    private String relationshipType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection
    @CollectionTable(name = "business_object_relationship_attributes",
                    joinColumns = @JoinColumn(name = "relationship_id"))
    @MapKeyColumn(name = "attribute_key")
    @Column(name = "attribute_value", columnDefinition = "TEXT")
    private Map<String, Object> attributes = new HashMap<>();

    @Column(nullable = false)
    private Boolean active = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 100)
    private String createdBy;

    @Column(length = 100)
    private String updatedBy;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BusinessObject getSource() {
        return source;
    }

    public void setSource(BusinessObject source) {
        this.source = source;
    }

    public BusinessObject getTarget() {
        return target;
    }

    public void setTarget(BusinessObject target) {
        this.target = target;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
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

    /**
     * Get relationship type display name
     */
    public String getRelationshipTypeDisplayName() {
        if (relationshipType == null) {
            return "";
        }
        return relationshipType.replace("_", " ").toLowerCase();
    }

    /**
     * Check if relationship is bidirectional
     */
    public boolean isBidirectional() {
        return relationshipType != null && (
            relationshipType.equalsIgnoreCase("RELATED_TO") ||
            relationshipType.equalsIgnoreCase("ASSOCIATED_WITH") ||
            relationshipType.equalsIgnoreCase("LINKED_TO")
        );
    }
}
