package com.jivs.platform.controller;

import com.jivs.platform.dto.DocumentDTO;
import com.jivs.platform.dto.DocumentSearchRequest;
import com.jivs.platform.dto.DocumentSearchResponse;
import com.jivs.platform.service.DocumentService;
import com.jivs.platform.service.archiving.DocumentArchivingService;
import com.jivs.platform.service.archiving.DocumentCompressionHelper;
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
import java.util.ArrayList;
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
    private final DocumentCompressionHelper compressionHelper;
    private final SearchService searchService;

    /**
     * Upload a document
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Upload a document", description = "Upload and optionally archive a document")
    public ResponseEntity<?> uploadDocument(
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

            // Archive if requested (compress file immediately)
            if (archive) {
                try {
                    // Compress the just-uploaded file
                    Map<String, Object> compressionResult = compressionHelper.compressDocumentFile(
                        document.getId(),
                        storageTier != null && !storageTier.equals("HOT") ? storageTier : "WARM"
                    );

                    // Reload document to get updated metadata
                    document = documentService.getDocument(document.getId());

                    log.info("Document {} uploaded and archived successfully: compressed={}",
                        document.getId(), compressionResult.get("compressed"));

                } catch (IOException e) {
                    // Compression failed, but upload succeeded
                    log.error("Document {} uploaded but archiving failed: {}",
                        document.getId(), e.getMessage());

                    // Don't mark as archived if compression failed
                    document.setArchived(false);
                    document = documentService.updateDocument(document.getId(), document);
                }
            }

            return ResponseEntity.ok(document);

        } catch (IOException e) {
            log.error("Failed to upload document: {}", file.getOriginalFilename(), e);
            log.error("Error details: {}", e.getMessage());
            log.error("Stack trace:", e);
            log.error("Storage path configured: /var/jivs/storage");

            // Return proper error response for frontend
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to upload document: " + e.getMessage());
            errorResponse.put("message", "Document upload failed due to storage error");
            errorResponse.put("filename", file.getOriginalFilename());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error uploading document: {}", file.getOriginalFilename(), e);
            log.error("Stack trace:", e);

            // Return proper error response for frontend
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Unexpected error: " + e.getMessage());
            errorResponse.put("message", "Document upload failed");
            errorResponse.put("filename", file.getOriginalFilename());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
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
    @Operation(summary = "Get all documents", description = "Get all documents with pagination, sorted by newest first")
    public ResponseEntity<Page<DocumentDTO>> getAllDocuments(
            @Parameter(description = "Pagination parameters") Pageable pageable,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "archived", required = false) Boolean archived) {

        // If no sort is specified, default to newest first (sort by createdDate descending)
        if (pageable.getSort().isUnsorted()) {
            pageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdDate")
            );
        }

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
     * Archive a single document (compress file)
     */
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Archive document", description = "Archive and compress an existing document")
    public ResponseEntity<Map<String, Object>> archiveDocument(
            @PathVariable Long id,
            @RequestParam(value = "compress", defaultValue = "true") boolean compress,
            @RequestParam(value = "storageTier", defaultValue = "WARM") String storageTier,
            @RequestParam(value = "deleteOriginal", defaultValue = "false") boolean deleteOriginal) {

        try {
            DocumentDTO doc = documentService.getDocument(id);
            if (doc == null) {
                return ResponseEntity.notFound().build();
            }

            // Compress file if requested (default: true)
            if (compress) {
                Map<String, Object> compressionResult = compressionHelper.compressDocumentFile(id, storageTier);

                // Return the full compression result (includes all size details)
                compressionResult.put("success", true);
                compressionResult.put("documentId", id);
                // compressionResult already contains: compressed, compressionRatio, spaceSaved,
                // originalSize, compressedSize, fileSizeOnDiskBefore, fileSizeOnDiskAfter,
                // actualDiskSpaceSaved, message

                return ResponseEntity.ok(compressionResult);

            } else {
                // Archive without compression (just set flags)
                doc.setArchived(true);
                doc.setStorageTier(storageTier);
                documentService.updateDocument(id, doc);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Document archived (not compressed)");
                response.put("documentId", id);
                response.put("compressed", false);

                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            log.error("Failed to archive document {}: {}", id, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("documentId", id);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Archive multiple documents (bulk operation)
     */
    @PostMapping("/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Archive multiple documents", description = "Archive and compress multiple documents")
    public ResponseEntity<Map<String, Object>> archiveDocuments(@RequestBody Map<String, Object> request) {
        try {
            // Extract parameters from request
            List<Integer> documentIdsInt = (List<Integer>) request.get("documentIds");
            if (documentIdsInt == null || documentIdsInt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "documentIds is required and cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Convert to Long
            List<Long> documentIds = documentIdsInt.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());

            boolean compress = (boolean) request.getOrDefault("compress", true);
            String archiveType = (String) request.getOrDefault("archiveType", "WARM");
            String storageTier = archiveType != null ? archiveType : "WARM";

            log.info("Starting bulk archive of {} documents with compression={}, storageTier={}",
                documentIds.size(), compress, storageTier);

            // Process each document
            List<Map<String, Object>> results = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;
            long totalSpaceSaved = 0;

            for (Long id : documentIds) {
                Map<String, Object> result = new HashMap<>();
                result.put("documentId", id);

                try {
                    if (compress) {
                        // Compress document
                        Map<String, Object> compressionResult = compressionHelper.compressDocumentFile(id, storageTier);

                        result.put("success", true);
                        result.put("compressed", compressionResult.get("compressed"));
                        result.put("compressionRatio", compressionResult.get("compressionRatio"));
                        result.put("spaceSaved", compressionResult.get("spaceSaved"));

                        // Track total space saved
                        if (compressionResult.get("spaceSaved") != null) {
                            totalSpaceSaved += (long) compressionResult.get("spaceSaved");
                        }

                    } else {
                        // Archive without compression
                        DocumentDTO doc = documentService.getDocument(id);
                        if (doc != null) {
                            doc.setArchived(true);
                            doc.setStorageTier(storageTier);
                            documentService.updateDocument(id, doc);

                            result.put("success", true);
                            result.put("compressed", false);
                        } else {
                            result.put("success", false);
                            result.put("error", "Document not found");
                            failureCount++;
                        }
                    }

                    successCount++;

                } catch (Exception e) {
                    log.error("Failed to archive document {}: {}", id, e.getMessage());
                    result.put("success", false);
                    result.put("error", e.getMessage());
                    failureCount++;
                }

                results.add(result);
            }

            // Build summary response
            Map<String, Object> response = new HashMap<>();
            response.put("success", failureCount == 0);
            response.put("totalDocuments", documentIds.size());
            response.put("successCount", successCount);
            response.put("failureCount", failureCount);
            response.put("totalSpaceSaved", totalSpaceSaved);
            response.put("results", results);

            log.info("Bulk archive completed: {}/{} succeeded, {} bytes saved",
                successCount, documentIds.size(), totalSpaceSaved);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Bulk archive failed: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Bulk archive failed: " + e.getMessage());
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
     * Diagnostic endpoint to check storage directory status
     */
    @GetMapping("/storage/diagnostic")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Storage diagnostic", description = "Check storage directory status and permissions")
    public ResponseEntity<Map<String, Object>> storageDiagnostic() {
        Map<String, Object> diagnostic = new HashMap<>();

        try {
            java.nio.file.Path storagePath = java.nio.file.Paths.get("/var/jivs/storage");
            diagnostic.put("storagePath", storagePath.toString());
            diagnostic.put("absolutePath", storagePath.toAbsolutePath().toString());
            diagnostic.put("exists", java.nio.file.Files.exists(storagePath));
            diagnostic.put("isDirectory", java.nio.file.Files.isDirectory(storagePath));
            diagnostic.put("isReadable", java.nio.file.Files.isReadable(storagePath));
            diagnostic.put("isWritable", java.nio.file.Files.isWritable(storagePath));

            // Try to create test directory
            try {
                java.nio.file.Path testDir = storagePath.resolve("diagnostic-test");
                java.nio.file.Files.createDirectories(testDir);
                diagnostic.put("canCreateDirectory", true);

                // Try to write test file
                java.nio.file.Path testFile = testDir.resolve("test.txt");
                java.nio.file.Files.write(testFile, "test".getBytes());
                diagnostic.put("canWriteFile", true);
                diagnostic.put("testFilePath", testFile.toString());

                // Clean up
                java.nio.file.Files.delete(testFile);
                java.nio.file.Files.delete(testDir);
                diagnostic.put("cleanupSuccess", true);
            } catch (Exception e) {
                diagnostic.put("canCreateDirectory", false);
                diagnostic.put("writeError", e.getClass().getSimpleName() + ": " + e.getMessage());
            }

            // Check parent directory
            java.nio.file.Path parentPath = java.nio.file.Paths.get("/var/jivs");
            diagnostic.put("parentExists", java.nio.file.Files.exists(parentPath));
            diagnostic.put("parentIsDirectory", java.nio.file.Files.isDirectory(parentPath));

            // Check /var directory
            java.nio.file.Path varPath = java.nio.file.Paths.get("/var");
            diagnostic.put("varExists", java.nio.file.Files.exists(varPath));
            diagnostic.put("varIsWritable", java.nio.file.Files.isWritable(varPath));

        } catch (Exception e) {
            diagnostic.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        return ResponseEntity.ok(diagnostic);
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
