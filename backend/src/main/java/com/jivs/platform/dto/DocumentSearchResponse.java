package com.jivs.platform.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for document search
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSearchResponse {
    private List<DocumentDTO> documents;
    private long totalHits;
    private Map<String, Map<String, Long>> facets;
    private boolean success;
    private String error;
    private long searchTime;
    private Map<String, List<String>> highlights;

    // Pagination metadata
    private int currentPage;
    private int pageSize;
    private int totalPages;

    // Search metadata
    private String query;
    private String sortBy;
    private String sortOrder;

    public DocumentSearchResponse(boolean success) {
        this.success = success;
    }

    public static DocumentSearchResponse error(String message) {
        DocumentSearchResponse response = new DocumentSearchResponse();
        response.setSuccess(false);
        response.error = message;  // Direct field access since we have @Data
        return response;
    }

    // Explicit getters and setters to work around Lombok compilation issues
    public List<DocumentDTO> getDocuments() { return documents; }
    public void setDocuments(List<DocumentDTO> documents) { this.documents = documents; }

    public long getTotalHits() { return totalHits; }
    public void setTotalHits(long totalHits) { this.totalHits = totalHits; }

    public Map<String, Map<String, Long>> getFacets() { return facets; }
    public void setFacets(Map<String, Map<String, Long>> facets) { this.facets = facets; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public long getSearchTime() { return searchTime; }
    public void setSearchTime(long searchTime) { this.searchTime = searchTime; }

    public Map<String, List<String>> getHighlights() { return highlights; }
    public void setHighlights(Map<String, List<String>> highlights) { this.highlights = highlights; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
}