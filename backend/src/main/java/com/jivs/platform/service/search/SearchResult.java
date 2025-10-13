package com.jivs.platform.service.search;

import java.util.Date;
import java.util.Map;

/**
 * Search result
 */
public class SearchResult {
    private String id;
    private EntityType type;
    private String title;
    private String description;
    private double score;
    private Date createdDate;
    private Date modifiedDate;
    private Map<String, Object> data;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public EntityType getType() { return type; }
    public void setType(EntityType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}
