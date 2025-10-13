package com.jivs.platform.service.search;

import java.util.Date;
import java.util.List;

/**
 * Document search filters
 */
public class DocumentSearchFilters {
    private String author;
    private Date dateFrom;
    private Date dateTo;
    private List<String> tags;

    // Getters and setters
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public Date getDateFrom() { return dateFrom; }
    public void setDateFrom(Date dateFrom) { this.dateFrom = dateFrom; }
    public Date getDateTo() { return dateTo; }
    public void setDateTo(Date dateTo) { this.dateTo = dateTo; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
