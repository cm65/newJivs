package com.jivs.platform.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for document search
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSearchRequest {
    private String query;
    private List<String> fileTypes;
    private Date dateFrom;
    private Date dateTo;
    private String author;
    private List<String> tags;
    private Long minSize;
    private Long maxSize;
    private Boolean archived;
    private String status;
    private String storageTier;
    private String sortBy = "relevance"; // relevance, date, title, size
    private String sortOrder = "desc"; // asc, desc
    private Integer from = 0;
    private Integer size = 20;
    private boolean includeFacets = true;
    private boolean includeHighlights = true;
    private Map<String, Object> filters;
}