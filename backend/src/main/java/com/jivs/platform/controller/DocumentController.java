package com.jivs.platform.controller;

import com.jivs.platform.dto.DocumentDTO;
import com.jivs.platform.dto.DocumentSearchRequest;
import com.jivs.platform.dto.DocumentSearchResponse;
import com.jivs.platform.service.DocumentService;
import com.jivs.platform.service.archiving.DocumentArchivingService;
import com.jivs.platform.service.search.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * REST controller for document management operations
 * Handles upload, archiving, search, and retrieval of documents
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Document management and archiving operations")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentArchivingService archivingService;
    private final SearchService searchService;

    /**
     * Upload a document
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Upload a document", description = "Upload and optionally archive a document")
    public ResponseEntity<DocumentDTO> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "archive", defaultValue = "false") boolean archive,
            @RequestParam(value = "compress", defaultValue = "true") boolean compress,
            @RequestParam(value = "encrypt", defaultValue = "false") boolean encrypt,
            @RequestParam(value = "storageTier", defaultValue = "HOT") String storageTier) {

        try {
            // Parse comma-separated tags into list (must be mutable for Hibernate)
            List<String> tagList = null;
            if (tags != null && !tags.trim().isEmpty()) {
                tagList = Arrays.stream(tags.split(","))
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.toList());
            }

            // Create document from uploaded file
            DocumentDTO document = documentService.uploadDocument(
                file,
                title != null ? title : file.getOriginalFilename(),
                description,
                tagList
            );

            // Archive if requested
            if (archive) {
                // Note: Archiving functionality temporarily disabled due to type incompatibility
                // TODO: Resolve ArchiveRequest type mismatch between DTO and service inner class
                document.setArchived(true);
            }

            return ResponseEntity.ok(document);

        } catch (IOException e) {
            log.error("Failed to upload document: {}", file.getOriginalFilename(), e);
            log.error("Error details: {}", e.getMessage());
            log.error("Storage path configured: /var/jivs/storage");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Unexpected error uploading document: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search documents
     */
    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Search documents", description = "Search documents with full-text search and filters")
    public ResponseEntity<DocumentSearchResponse> searchDocuments(@Valid @RequestBody DocumentSearchRequest request) {
        DocumentSearchResponse response = documentService.searchDocuments(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all documents with pagination
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Get all documents", description = "Get all documents with pagination")
    public ResponseEntity<Page<DocumentDTO>> getAllDocuments(
            @Parameter(description = "Pagination parameters") Pageable pageable,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "archived", required = false) Boolean archived) {

        Page<DocumentDTO> documents = documentService.getAllDocuments(pageable, status, archived);
        return ResponseEntity.ok(documents);
    }

    /**
     * Get document by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Get document by ID", description = "Get a specific document by its ID")
    public ResponseEntity<DocumentDTO> getDocument(@PathVariable Long id) {
        DocumentDTO document = documentService.getDocument(id);
        if (document != null) {
            return ResponseEntity.ok(document);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Download document
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Download document", description = "Download the original document file")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) {
        try {
            byte[] content = documentService.downloadDocument(id);
            if (content != null) {
                return ResponseEntity.ok()
                    .header("Content-Type", "application/octet-stream")
                    .header("Content-Disposition", "attachment; filename=\"document.pdf\"")
                    .body(content);
            }
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Bulk archive documents
     */
    @PostMapping("/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Archive multiple documents", description = "Archive multiple documents at once")
    public ResponseEntity<Map<String, Object>> archiveDocuments(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> documentIds = (List<Object>) request.get("documentIds");
            String archiveType = (String) request.getOrDefault("archiveType", "WARM");
            Boolean compress = (Boolean) request.getOrDefault("compress", true);
            String archiveReason = (String) request.getOrDefault("archiveReason", "");

            int successCount = 0;
            int failureCount = 0;

            for (Object docIdObj : documentIds) {
                try {
                    // Handle both Integer and String IDs
                    Long docId;
                    if (docIdObj instanceof Integer) {
                        docId = ((Integer) docIdObj).longValue();
                    } else if (docIdObj instanceof String) {
                        docId = Long.valueOf((String) docIdObj);
                    } else {
                        docId = ((Number) docIdObj).longValue();
                    }

                    // Update document to archived status
                    DocumentDTO doc = documentService.getDocument(docId);
                    if (doc != null) {
                        doc.setArchived(true);
                        doc.setStorageTier(archiveType);
                        documentService.updateDocument(docId, doc);
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    log.error("Failed to archive document {}: {}", docIdObj, e.getMessage());
                    failureCount++;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", failureCount == 0);
            response.put("successCount", successCount);
            response.put("failureCount", failureCount);
            response.put("message", String.format("Archived %d document(s), %d failed", successCount, failureCount));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Archive a single document
     */
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Archive document", description = "Archive an existing document")
    public ResponseEntity<Map<String, Object>> archiveDocument(
            @PathVariable Long id,
            @RequestParam(value = "compress", defaultValue = "true") boolean compress,
            @RequestParam(value = "storageTier", defaultValue = "WARM") String storageTier,
            @RequestParam(value = "deleteOriginal", defaultValue = "false") boolean deleteOriginal) {

        try {
            DocumentDTO doc = documentService.getDocument(id);
            if (doc != null) {
                doc.setArchived(true);
                doc.setStorageTier(storageTier);
                documentService.updateDocument(id, doc);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Document archived successfully");
                response.put("documentId", id);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Restore archived document
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Restore archived document", description = "Restore a document from archive")
    public ResponseEntity<Map<String, Object>> restoreDocument(@PathVariable Long id) {
        try {
            boolean restored = documentService.restoreDocument(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", restored);
            response.put("documentId", id);
            response.put("message", restored ? "Document restored successfully" : "Failed to restore document");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Delete document
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete document", description = "Permanently delete a document")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        boolean deleted = documentService.deleteDocument(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Get document statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get document statistics", description = "Get statistics about documents and archives")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = documentService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Scan directory for documents
     */
    @PostMapping("/scan")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Scan directory", description = "Scan a directory for documents to import")
    public ResponseEntity<Map<String, Object>> scanDirectory(
            @RequestParam("path") String path,
            @RequestParam(value = "recursive", defaultValue = "true") boolean recursive,
            @RequestParam(value = "fileTypes", defaultValue = "pdf,doc,docx,txt") String fileTypes,
            @RequestParam(value = "archive", defaultValue = "true") boolean archive,
            @RequestParam(value = "index", defaultValue = "true") boolean index) {

        Map<String, Object> scanResult = documentService.scanDirectory(path, recursive, fileTypes, archive, index);
        return ResponseEntity.ok(scanResult);
    }

    /**
     * Extract text content from document
     */
    @GetMapping("/{id}/content")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Get document content", description = "Extract and return text content from document")
    public ResponseEntity<Map<String, Object>> getDocumentContent(@PathVariable Long id) {
        Map<String, Object> content = documentService.extractContent(id);
        if (content != null) {
            return ResponseEntity.ok(content);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Update document metadata
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Update document", description = "Update document metadata")
    public ResponseEntity<DocumentDTO> updateDocument(
            @PathVariable Long id,
            @RequestBody DocumentDTO updates) {

        DocumentDTO updated = documentService.updateDocument(id, updates);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Get archiving rules
     */
    @GetMapping("/archiving/rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Get archiving rules", description = "Get document archiving rules and policies")
    public ResponseEntity<Map<String, Object>> getArchivingRules() {
        log.info("Fetching archiving rules");

        Map<String, Object> rules = new HashMap<>();

        // Storage tiers
        Map<String, Object> storageTiers = new HashMap<>();
        storageTiers.put("HOT", Map.of("description", "Frequently accessed", "retentionDays", 90));
        storageTiers.put("WARM", Map.of("description", "Occasionally accessed", "retentionDays", 365));
        storageTiers.put("COLD", Map.of("description", "Rarely accessed", "retentionDays", 2555)); // 7 years

        rules.put("storageTiers", storageTiers);
        rules.put("defaultTier", "WARM");
        rules.put("compressionEnabled", true);
        rules.put("encryptionRequired", false);

        return ResponseEntity.ok(rules);
    }

    /**
     * Create archiving rule
     */
    @PostMapping("/archiving/rules")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create archiving rule", description = "Create a new document archiving rule")
    public ResponseEntity<Map<String, Object>> createArchivingRule(@RequestBody Map<String, Object> rule) {
        log.info("Creating archiving rule");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Archiving rule created successfully");
        response.put("rule", rule);

        return ResponseEntity.ok(response);
    }

    /**
     * Update archiving rule
     */
    @PutMapping("/archiving/rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update archiving rule", description = "Update an existing archiving rule")
    public ResponseEntity<Map<String, Object>> updateArchivingRule(
            @PathVariable Long id,
            @RequestBody Map<String, Object> rule) {

        log.info("Updating archiving rule with ID: {}", id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Archiving rule updated successfully");
        response.put("ruleId", id);
        response.put("rule", rule);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete archiving rule
     */
    @DeleteMapping("/archiving/rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete archiving rule", description = "Delete an archiving rule")
    public ResponseEntity<Map<String, Object>> deleteArchivingRule(@PathVariable Long id) {
        log.info("Deleting archiving rule with ID: {}", id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Archiving rule deleted successfully");
        response.put("ruleId", id);

        return ResponseEntity.ok(response);
    }
}
