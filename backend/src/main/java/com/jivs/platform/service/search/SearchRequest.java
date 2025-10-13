package com.jivs.platform.service.search;

import java.util.Map;

/**
 * Search request
 */
public class SearchRequest {
    private String query;
    private EntityType entityType;
    private Map<String, Object> filters;
    private String sortBy;
    private SortOrder sortOrder;
    private Integer from;
    private Integer size;
    private boolean includeFacets;
    private boolean includeHighlights;

    // Getters and setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public EntityType getEntityType() { return entityType; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; }
    public Map<String, Object> getFilters() { return filters; }
    public void setFilters(Map<String, Object> filters) { this.filters = filters; }
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public SortOrder getSortOrder() { return sortOrder; }
    public void setSortOrder(SortOrder sortOrder) { this.sortOrder = sortOrder; }
    public Integer getFrom() { return from; }
    public void setFrom(Integer from) { this.from = from; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    public boolean isIncludeFacets() { return includeFacets; }
    public void setIncludeFacets(boolean includeFacets) { this.includeFacets = includeFacets; }
    public boolean isIncludeHighlights() { return includeHighlights; }
    public void setIncludeHighlights(boolean includeHighlights) {
        this.includeHighlights = includeHighlights;
    }
}
