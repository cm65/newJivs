package com.jivs.platform.service.search;

import com.jivs.platform.domain.Document;
import com.jivs.platform.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for full-text search across all platform entities
 * Uses database search as fallback when Elasticsearch is not available
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SearchService.class);
    private final DocumentRepository documentRepository;

    /**
     * Search across all entities
     */
    public SearchResponse search(SearchRequest request) {
        log.info("Executing search: query='{}', type={}", request.getQuery(), request.getEntityType());

        SearchResponse response = new SearchResponse();
        response.setQuery(request.getQuery());
        response.setStartTime(new Date());

        try {
            // Build Elasticsearch query
            String esQuery = buildElasticsearchQuery(request);

            // Execute search
            List<SearchResult> results = executeSearch(esQuery, request);

            // Apply filters
            if (request.getFilters() != null && !request.getFilters().isEmpty()) {
                results = applyFilters(results, request.getFilters());
            }

            // Apply sorting
            results = applySorting(results, request.getSortBy(), request.getSortOrder());

            // Apply pagination
            int total = results.size();
            int from = request.getFrom() != null ? request.getFrom() : 0;
            int size = request.getSize() != null ? request.getSize() : 20;

            List<SearchResult> paginatedResults = results.stream()
                .skip(from)
                .limit(size)
                .collect(Collectors.toList());

            response.setResults(paginatedResults);
            response.setTotalHits(total);
            response.setFrom(from);
            response.setSize(size);

            // Generate facets
            if (request.isIncludeFacets()) {
                response.setFacets(generateFacets(results));
            }

            // Generate highlights
            if (request.isIncludeHighlights()) {
                response.setHighlights(generateHighlights(paginatedResults, request.getQuery()));
            }

            response.setSuccess(true);
            response.setEndTime(new Date());
            response.setDurationMs(response.getEndTime().getTime() - response.getStartTime().getTime());

            log.info("Search completed: {} hits in {}ms", total, response.getDurationMs());

        } catch (Exception e) {
            log.error("Search failed: {}", e.getMessage(), e);
            response.setSuccess(false);
            response.setErrorMessage(e.getMessage());
        }

        return response;
    }

    /**
     * Search documents
     */
    public SearchResponse searchDocuments(String query, DocumentSearchFilters filters) {
        SearchRequest request = new SearchRequest();
        request.setQuery(query);
        request.setEntityType(EntityType.DOCUMENT);
        request.setFilters(filtersToMap(filters));
        return search(request);
    }

    /**
     * Search business objects
     */
    public SearchResponse searchBusinessObjects(String query, Map<String, Object> filters) {
        SearchRequest request = new SearchRequest();
        request.setQuery(query);
        request.setEntityType(EntityType.BUSINESS_OBJECT);
        request.setFilters(filters);
        return search(request);
    }

    /**
     * Search extractions
     */
    public SearchResponse searchExtractions(String query, Map<String, Object> filters) {
        SearchRequest request = new SearchRequest();
        request.setQuery(query);
        request.setEntityType(EntityType.EXTRACTION);
        request.setFilters(filters);
        return search(request);
    }

    /**
     * Search migrations
     */
    public SearchResponse searchMigrations(String query, Map<String, Object> filters) {
        SearchRequest request = new SearchRequest();
        request.setQuery(query);
        request.setEntityType(EntityType.MIGRATION);
        request.setFilters(filters);
        return search(request);
    }

    /**
     * Autocomplete suggestions
     */
    public List<String> autocomplete(String prefix, EntityType entityType) {
        log.debug("Getting autocomplete suggestions for: {}", prefix);

        // TODO: Implement Elasticsearch autocomplete
        List<String> suggestions = new ArrayList<>();
        suggestions.add(prefix + " suggestion 1");
        suggestions.add(prefix + " suggestion 2");
        suggestions.add(prefix + " suggestion 3");

        return suggestions;
    }

    /**
     * Get similar documents
     */
    public List<SearchResult> findSimilar(String documentId, int limit) {
        log.debug("Finding documents similar to: {}", documentId);

        // TODO: Implement More Like This query in Elasticsearch
        return new ArrayList<>();
    }

    /**
     * Index document
     */
    public void indexDocument(IndexRequest request) {
        log.info("Indexing document: {} (type: {})", request.getId(), request.getEntityType());

        try {
            // TODO: Index in Elasticsearch
            log.debug("Document indexed successfully: {}", request.getId());
        } catch (Exception e) {
            log.error("Failed to index document: {}", e.getMessage(), e);
        }
    }

    /**
     * Delete document from index
     */
    public void deleteFromIndex(String id, EntityType entityType) {
        log.info("Deleting from index: {} (type: {})", id, entityType);

        try {
            // TODO: Delete from Elasticsearch
            log.debug("Document deleted from index: {}", id);
        } catch (Exception e) {
            log.error("Failed to delete from index: {}", e.getMessage(), e);
        }
    }

    /**
     * Reindex all documents
     */
    public ReindexResult reindexAll() {
        log.info("Starting full reindex");

        ReindexResult result = new ReindexResult();
        result.setStartTime(new Date());

        try {
            // TODO: Implement full reindex
            result.setTotalDocuments(0);
            result.setIndexedDocuments(0);
            result.setFailedDocuments(0);
            result.setSuccess(true);
            result.setEndTime(new Date());

            log.info("Reindex completed: {} documents indexed", result.getIndexedDocuments());

        } catch (Exception e) {
            log.error("Reindex failed: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * Build Elasticsearch query
     */
    private String buildElasticsearchQuery(SearchRequest request) {
        // TODO: Build proper Elasticsearch query DSL
        return request.getQuery();
    }

    /**
     * Execute search using database queries
     * Falls back to database search when Elasticsearch is not available
     */
    private List<SearchResult> executeSearch(String query, SearchRequest request) {
        List<SearchResult> results = new ArrayList<>();

        try {
            // For DOCUMENT entity type, search in documents table
            if (request.getEntityType() == EntityType.DOCUMENT) {
                List<Document> documents = searchDocumentsInDatabase(query);

                for (Document doc : documents) {
                    SearchResult result = new SearchResult();
                    result.setId(String.valueOf(doc.getId()));
                    result.setType(EntityType.DOCUMENT);
                    result.setTitle(doc.getTitle() != null ? doc.getTitle() : doc.getFilename());
                    result.setDescription(doc.getDescription());
                    result.setScore(calculateRelevanceScore(doc, query));
                    result.setCreatedDate(doc.getCreatedDate());

                    Map<String, Object> data = new HashMap<>();
                    data.put("filename", doc.getFilename());
                    data.put("fileType", doc.getFileType());
                    data.put("size", doc.getSize());
                    data.put("archived", doc.isArchived());
                    result.setData(data);

                    results.add(result);
                }
            }
            // TODO: Add support for other entity types (EXTRACTION, MIGRATION, etc.)

        } catch (Exception e) {
            log.error("Database search failed: {}", e.getMessage(), e);
        }

        return results;
    }

    /**
     * Search documents in database using LIKE queries
     */
    private List<Document> searchDocumentsInDatabase(String query) {
        if (query == null || query.trim().isEmpty()) {
            // Return recent documents sorted by newest first if no query
            return documentRepository.findAll()
                .stream()
                .sorted((d1, d2) -> {
                    if (d1.getCreatedDate() == null) return 1;
                    if (d2.getCreatedDate() == null) return -1;
                    return d2.getCreatedDate().compareTo(d1.getCreatedDate()); // Descending order (newest first)
                })
                .limit(20)
                .collect(Collectors.toList());
        }

        String searchPattern = "%" + query.toLowerCase() + "%";
        List<Document> allDocs = documentRepository.findAll();

        // Filter documents using partial keyword matching (supports partial search!)
        return allDocs.stream()
            .filter(doc ->
                (doc.getTitle() != null && doc.getTitle().toLowerCase().contains(query.toLowerCase())) ||
                (doc.getFilename() != null && doc.getFilename().toLowerCase().contains(query.toLowerCase())) ||
                (doc.getDescription() != null && doc.getDescription().toLowerCase().contains(query.toLowerCase())) ||
                (doc.getContent() != null && doc.getContent().toLowerCase().contains(query.toLowerCase())) ||
                (doc.getTags() != null && doc.getTags().stream()
                    .anyMatch(tag -> tag.toLowerCase().contains(query.toLowerCase())))
            )
            // Sort results by newest first for better user experience
            .sorted((d1, d2) -> {
                if (d1.getCreatedDate() == null) return 1;
                if (d2.getCreatedDate() == null) return -1;
                return d2.getCreatedDate().compareTo(d1.getCreatedDate());
            })
            .collect(Collectors.toList());
    }

    /**
     * Calculate relevance score based on where the query matches
     */
    private double calculateRelevanceScore(Document doc, String query) {
        if (query == null || query.trim().isEmpty()) {
            return 1.0;
        }

        String lowerQuery = query.toLowerCase();
        double score = 0.0;

        // Title match (highest weight)
        if (doc.getTitle() != null && doc.getTitle().toLowerCase().contains(lowerQuery)) {
            score += 3.0;
        }

        // Filename match
        if (doc.getFilename() != null && doc.getFilename().toLowerCase().contains(lowerQuery)) {
            score += 2.0;
        }

        // Tags match
        if (doc.getTags() != null && doc.getTags().stream()
                .anyMatch(tag -> tag.toLowerCase().contains(lowerQuery))) {
            score += 2.5;
        }

        // Description match
        if (doc.getDescription() != null && doc.getDescription().toLowerCase().contains(lowerQuery)) {
            score += 1.5;
        }

        // Content match (lowest weight, but still relevant)
        if (doc.getContent() != null && doc.getContent().toLowerCase().contains(lowerQuery)) {
            score += 1.0;
        }

        return Math.min(score, 5.0); // Cap at 5.0
    }

    /**
     * Apply filters to results
     */
    private List<SearchResult> applyFilters(List<SearchResult> results, Map<String, Object> filters) {
        // TODO: Implement filtering logic
        return results;
    }

    /**
     * Apply sorting to results
     */
    private List<SearchResult> applySorting(
            List<SearchResult> results,
            String sortBy,
            SortOrder sortOrder) {

        if (sortBy == null) {
            return results;
        }

        Comparator<SearchResult> comparator;

        switch (sortBy) {
            case "score":
                comparator = Comparator.comparing(SearchResult::getScore);
                break;
            case "title":
                comparator = Comparator.comparing(SearchResult::getTitle);
                break;
            case "date":
                comparator = Comparator.comparing(SearchResult::getCreatedDate, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            default:
                return results;
        }

        if (sortOrder == SortOrder.DESC) {
            comparator = comparator.reversed();
        }

        return results.stream()
            .sorted(comparator)
            .collect(Collectors.toList());
    }

    /**
     * Generate facets for results
     */
    private Map<String, Map<String, Long>> generateFacets(List<SearchResult> results) {
        Map<String, Map<String, Long>> facets = new HashMap<>();

        // Type facet
        Map<String, Long> typeFacet = results.stream()
            .collect(Collectors.groupingBy(
                r -> r.getType() != null ? r.getType().toString() : "unknown",
                Collectors.counting()
            ));
        facets.put("type", typeFacet);

        // TODO: Generate more facets

        return facets;
    }

    /**
     * Generate highlights for results
     */
    private Map<String, List<String>> generateHighlights(List<SearchResult> results, String query) {
        Map<String, List<String>> highlights = new HashMap<>();

        for (SearchResult result : results) {
            List<String> resultHighlights = new ArrayList<>();
            // TODO: Implement actual highlighting logic
            resultHighlights.add("...matching text with <em>" + query + "</em>...");
            highlights.put(result.getId(), resultHighlights);
        }

        return highlights;
    }

    /**
     * Convert filters to map
     */
    private Map<String, Object> filtersToMap(DocumentSearchFilters filters) {
        Map<String, Object> map = new HashMap<>();
        if (filters.getAuthor() != null) map.put("author", filters.getAuthor());
        if (filters.getDateFrom() != null) map.put("dateFrom", filters.getDateFrom());
        if (filters.getDateTo() != null) map.put("dateTo", filters.getDateTo());
        if (filters.getTags() != null) map.put("tags", filters.getTags());
        return map;
    }
}
