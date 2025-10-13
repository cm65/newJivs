package com.jivs.platform.service.search;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Search response
 */
public class SearchResponse {
    private String query;
    private List<SearchResult> results;
    private int totalHits;
    private int from;
    private int size;
    private Map<String, Map<String, Long>> facets;
    private Map<String, List<String>> highlights;
    private boolean success;
    private String errorMessage;
    private Date startTime;
    private Date endTime;
    private long durationMs;

    // Getters and setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public List<SearchResult> getResults() { return results; }
    public void setResults(List<SearchResult> results) { this.results = results; }
    public int getTotalHits() { return totalHits; }
    public void setTotalHits(int totalHits) { this.totalHits = totalHits; }
    public int getFrom() { return from; }
    public void setFrom(int from) { this.from = from; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public Map<String, Map<String, Long>> getFacets() { return facets; }
    public void setFacets(Map<String, Map<String, Long>> facets) { this.facets = facets; }
    public Map<String, List<String>> getHighlights() { return highlights; }
    public void setHighlights(Map<String, List<String>> highlights) { this.highlights = highlights; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
}
