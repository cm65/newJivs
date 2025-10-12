package com.jivs.platform.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Entity representing a saved view with filters, sorting, and column preferences.
 * Part of Sprint 2 - Workflow 7: Advanced Filtering Implementation
 */
@Entity
@Table(name = "saved_views")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "module", nullable = false, length = 50)
    private String module;

    @Column(name = "view_name", nullable = false, length = 100)
    private String viewName;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Type(JsonType.class)
    @Column(name = "filters", columnDefinition = "jsonb")
    private Map<String, Object> filters;

    @Type(JsonType.class)
    @Column(name = "sorting", columnDefinition = "jsonb")
    private Map<String, String> sorting;

    @Type(JsonType.class)
    @Column(name = "visible_columns", columnDefinition = "jsonb")
    private List<String> visibleColumns;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Validate module value
     */
    public void setModule(String module) {
        if (module != null && !isValidModule(module)) {
            throw new IllegalArgumentException("Module must be one of: extractions, migrations, data-quality, compliance");
        }
        this.module = module;
    }

    /**
     * Check if module is valid
     */
    private boolean isValidModule(String module) {
        return module.equals("extractions") ||
               module.equals("migrations") ||
               module.equals("data-quality") ||
               module.equals("compliance");
    }

    /**
     * Set as default view
     * Note: Business logic should ensure only one default view per user per module
     */
    public void setAsDefault() {
        this.isDefault = true;
    }

    /**
     * Unset as default view
     */
    public void unsetAsDefault() {
        this.isDefault = false;
    }
}
