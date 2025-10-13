package com.jivs.platform.service.search;

import java.util.Map;

/**
 * Index request
 */
public class IndexRequest {
    private String id;
    private EntityType entityType;
    private Map<String, Object> data;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public EntityType getEntityType() { return entityType; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}
