package com.jivs.platform.service;

import com.jivs.platform.domain.Document;
import com.jivs.platform.dto.DocumentDTO;
import com.jivs.platform.dto.DocumentSearchRequest;
import com.jivs.platform.dto.DocumentSearchResponse;
import com.jivs.platform.repository.DocumentRepository;
import com.jivs.platform.service.storage.StorageService;
import com.jivs.platform.service.storage.StorageOptions;
import com.jivs.platform.service.storage.StorageResult;
import com.jivs.platform.service.storage.FileData;
import com.jivs.platform.service.search.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for document management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    // Explicit log field to work around Lombok @Slf4j compilation issue
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final StorageService storageService;
    private final SearchService searchService;

    /**
     * Upload a document
     */
    @Transactional
    public DocumentDTO uploadDocument(MultipartFile file, String title, String description, List<String> tags) throws IOException {
        log.info("Uploading document: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());

        // Create document entity
        Document document = new Document();
        document.setFilename(file.getOriginalFilename());
        document.setTitle(title);
        document.setDescription(description);
        document.setFileType(getFileType(file.getOriginalFilename()));
        document.setSize(file.getSize());
        document.setTags(tags);
        document.setStatus("ACTIVE");
        document.setCreatedDate(new Date());
        document.setModifiedDate(new Date());

        // Calculate checksum
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(file.getBytes());
            document.setChecksum(Base64.getEncoder().encodeToString(hash));
        } catch (Exception e) {
            log.error("Error calculating checksum: {}", e.getMessage());
        }

        // Extract content for searchability
        String content = extractTextContent(file.getBytes(), file.getOriginalFilename());
        document.setContent(content);

        // Save to database first to get the ID
        document = documentRepository.save(document);

        // Store file using the generated ID
        StorageOptions storageOpts = new StorageOptions();
        storageOpts.setDirectory("documents");
        StorageResult storageResult = storageService.storeFile(String.valueOf(document.getId()), file.getBytes(), storageOpts);
        String storagePath = storageResult.getPath();
        document.setStoragePath(storagePath);

        // Save again to update storage path
        document = documentRepository.save(document);

        // Index for search
        indexDocument(document);

        log.info("Document uploaded successfully: {}", document.getId());
        return toDTO(document);
    }

    /**
     * Search documents
     */
    public DocumentSearchResponse searchDocuments(DocumentSearchRequest request) {
        log.info("Searching documents with query: {}", request.getQuery());

        DocumentSearchResponse response = new DocumentSearchResponse();

        try {
            // Use search service for full-text search
            com.jivs.platform.service.search.SearchRequest searchReq = new com.jivs.platform.service.search.SearchRequest();
            searchReq.setQuery(request.getQuery());
            searchReq.setEntityType(com.jivs.platform.service.search.EntityType.DOCUMENT);
            searchReq.setFrom(request.getFrom());
            searchReq.setSize(request.getSize());
            searchReq.setFilters(request.getFilters());

            com.jivs.platform.service.search.SearchResponse searchResult = searchService.search(searchReq);

            // Convert results to DTOs
            List<DocumentDTO> documents = new ArrayList<>();
            for (com.jivs.platform.service.search.SearchResult result : searchResult.getResults()) {
                Document doc = documentRepository.findById(Long.valueOf(result.getId())).orElse(null);
                if (doc != null) {
                    DocumentDTO dto = toDTO(doc);
                    dto.setScore(result.getScore());
                    // Set highlight if available
                    if (searchResult.getHighlights() != null) {
                        dto.setHighlight(searchResult.getHighlights().get(result.getId()));
                    }
                    documents.add(dto);
                }
            }

            response.setDocuments(documents);
            response.setTotalHits(searchResult.getTotalHits());
            response.setFacets(searchResult.getFacets());
            response.setSuccess(true);

        } catch (Exception e) {
            log.error("Search failed: {}", e.getMessage(), e);
            response.setSuccess(false);
            response.setError(e.getMessage());
        }

        return response;
    }

    /**
     * Get all documents with pagination
     */
    public Page<DocumentDTO> getAllDocuments(Pageable pageable, String status, Boolean archived) {
        Page<Document> documents;

        if (status != null && archived != null) {
            documents = documentRepository.findByStatusAndArchived(status, archived, pageable);
        } else if (status != null) {
            documents = documentRepository.findByStatus(status, pageable);
        } else if (archived != null) {
            documents = documentRepository.findByArchived(archived, pageable);
        } else {
            documents = documentRepository.findAll(pageable);
        }

        return documents.map(this::toDTO);
    }

    /**
     * Get document by ID
     */
    public DocumentDTO getDocument(Long id) {
        Optional<Document> document = documentRepository.findById(id);
        return document.map(this::toDTO).orElse(null);
    }

    /**
     * Download document
     */
    public byte[] downloadDocument(Long id) throws IOException {
        Optional<Document> document = documentRepository.findById(id);
        if (document.isPresent()) {
            // Read file directly from storage path since metadata lookup is not implemented
            String storagePath = document.get().getStoragePath();
            if (storagePath != null && !storagePath.isEmpty()) {
                Path filePath = Paths.get(storagePath);
                if (Files.exists(filePath)) {
                    return Files.readAllBytes(filePath);
                } else {
                    log.error("File not found at path: {}", storagePath);
                    throw new FileNotFoundException("File not found at path: " + storagePath);
                }
            }
        }
        return null;
    }

    /**
     * Restore document from archive
     */
    @Transactional
    public boolean restoreDocument(Long id) {
        Optional<Document> document = documentRepository.findById(id);
        if (document.isPresent()) {
            Document doc = document.get();
            doc.setArchived(false);
            doc.setStatus("ACTIVE");
            doc.setModifiedDate(new Date());
            documentRepository.save(doc);
            log.info("Document restored: {}", id);
            return true;
        }
        return false;
    }

    /**
     * Delete document
     */
    @Transactional
    public boolean deleteDocument(Long id) {
        Optional<Document> document = documentRepository.findById(id);
        if (document.isPresent()) {
            Document doc = document.get();

            // Delete from storage
            try {
                String storagePath = doc.getStoragePath();
                String storageId = storagePath.contains("/") ? storagePath.substring(storagePath.lastIndexOf('/') + 1) : storagePath;
                storageService.deleteFile(storageId);
            } catch (Exception e) {
                log.error("Error deleting file from storage: {}", e.getMessage());
            }

            // Delete from database
            documentRepository.delete(doc);

            // Remove from search index
            searchService.deleteFromIndex(String.valueOf(id), com.jivs.platform.service.search.EntityType.DOCUMENT);

            log.info("Document deleted: {}", id);
            return true;
        }
        return false;
    }

    /**
     * Update document metadata
     */
    @Transactional
    public DocumentDTO updateDocument(Long id, DocumentDTO updates) {
        Optional<Document> documentOpt = documentRepository.findById(id);
        if (documentOpt.isPresent()) {
            Document document = documentOpt.get();

            if (updates.getTitle() != null) {
                document.setTitle(updates.getTitle());
            }
            if (updates.getDescription() != null) {
                document.setDescription(updates.getDescription());
            }
            if (updates.getTags() != null) {
                document.setTags(updates.getTags());
            }

            // Support archiving/unarchiving
            document.setArchived(updates.isArchived());

            // Support storage tier changes
            if (updates.getStorageTier() != null) {
                document.setStorageTier(updates.getStorageTier());
            }

            document.setModifiedDate(new Date());
            document = documentRepository.save(document);

            // Re-index for search
            indexDocument(document);

            return toDTO(document);
        }
        return null;
    }

    /**
     * Get document statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalDocuments", documentRepository.count());
        stats.put("activeDocuments", documentRepository.countByArchived(false));
        stats.put("archivedDocuments", documentRepository.countByArchived(true));

        // Calculate total size of all documents
        List<Document> allDocuments = documentRepository.findAll();
        long totalSize = allDocuments.stream()
            .mapToLong(doc -> doc.getSize() != null ? doc.getSize() : 0L)
            .sum();
        stats.put("totalSize", totalSize);

        // Get storage size by type
        Map<String, Long> sizeByType = new HashMap<>();
        sizeByType.put("PDF", documentRepository.sumSizeByFileType("pdf"));
        sizeByType.put("DOC", documentRepository.sumSizeByFileType("doc"));
        sizeByType.put("DOCX", documentRepository.sumSizeByFileType("docx"));
        sizeByType.put("TXT", documentRepository.sumSizeByFileType("txt"));
        stats.put("sizeByType", sizeByType);

        // Get document count by type
        Map<String, Long> countByType = new HashMap<>();
        countByType.put("PDF", documentRepository.countByFileType("pdf"));
        countByType.put("DOC", documentRepository.countByFileType("doc"));
        countByType.put("DOCX", documentRepository.countByFileType("docx"));
        countByType.put("TXT", documentRepository.countByFileType("txt"));
        stats.put("countByType", countByType);

        return stats;
    }

    /**
     * Scan directory for documents
     */
    public Map<String, Object> scanDirectory(String path, boolean recursive, String fileTypes, boolean archive, boolean index) {
        Map<String, Object> result = new HashMap<>();
        List<String> processedFiles = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();
        int totalFiles = 0;

        try {
            Path directory = Paths.get(path);
            if (!Files.exists(directory) || !Files.isDirectory(directory)) {
                result.put("error", "Directory does not exist or is not accessible");
                return result;
            }

            Set<String> allowedTypes = new HashSet<>(Arrays.asList(fileTypes.split(",")));

            Stream<Path> fileStream = recursive ?
                Files.walk(directory) :
                Files.list(directory);

            List<Path> files = fileStream
                .filter(Files::isRegularFile)
                .filter(file -> {
                    String fileName = file.getFileName().toString().toLowerCase();
                    return allowedTypes.stream().anyMatch(fileName::endsWith);
                })
                .collect(Collectors.toList());

            totalFiles = files.size();

            for (Path file : files) {
                try {
                    // Read file
                    byte[] content = Files.readAllBytes(file);

                    // Create document
                    Document document = new Document();
                    document.setFilename(file.getFileName().toString());
                    document.setTitle(file.getFileName().toString());
                    document.setPath(file.toString());
                    document.setFileType(getFileType(file.getFileName().toString()));
                    document.setSize((long) content.length);
                    document.setStatus("ACTIVE");
                    document.setCreatedDate(new Date());

                    // Extract content if indexing requested
                    if (index) {
                        String textContent = extractTextContent(content, file.getFileName().toString());
                        document.setContent(textContent);
                    }

                    // Save to database first to get the ID
                    document = documentRepository.save(document);

                    // Store file using the generated ID
                    StorageOptions opts = new StorageOptions();
                    opts.setDirectory("documents");
                    StorageResult res = storageService.storeFile(String.valueOf(document.getId()), content, opts);
                    String storagePath = res.getPath();
                    document.setStoragePath(storagePath);

                    // Save again to update storage path
                    document = documentRepository.save(document);

                    // Index for search if requested
                    if (index) {
                        indexDocument(document);
                    }

                    processedFiles.add(file.getFileName().toString());

                } catch (Exception e) {
                    log.error("Failed to process file: {}", file, e);
                    failedFiles.add(file.getFileName().toString());
                }
            }

        } catch (Exception e) {
            log.error("Directory scan failed: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
        }

        result.put("totalFiles", totalFiles);
        result.put("processedFiles", processedFiles.size());
        result.put("failedFiles", failedFiles.size());
        result.put("files", processedFiles);
        result.put("failures", failedFiles);

        return result;
    }

    /**
     * Extract content from document
     */
    public Map<String, Object> extractContent(Long id) {
        Optional<Document> document = documentRepository.findById(id);
        if (document.isPresent()) {
            Map<String, Object> content = new HashMap<>();
            content.put("id", document.get().getId());
            content.put("filename", document.get().getFilename());
            content.put("content", document.get().getContent());
            content.put("wordCount", countWords(document.get().getContent()));
            return content;
        }
        return null;
    }

    /**
     * Extract text content from file
     */
    private String extractTextContent(byte[] fileContent, String filename) {
        String lowerFilename = filename.toLowerCase();

        try {
            if (lowerFilename.endsWith(".pdf")) {
                return extractPdfContent(fileContent);
            } else if (lowerFilename.endsWith(".docx")) {
                return extractDocxContent(fileContent);
            } else if (lowerFilename.endsWith(".txt")) {
                return new String(fileContent);
            }
        } catch (Exception e) {
            log.error("Error extracting content from {}: {}", filename, e.getMessage());
        }

        return "";
    }

    /**
     * Extract content from PDF
     */
    private String extractPdfContent(byte[] content) throws IOException {
        StringBuilder text = new StringBuilder();

        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(content)))) {
            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                text.append(PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i)));
                text.append("\n");
            }
        }

        return text.toString();
    }

    /**
     * Extract content from DOCX
     */
    private String extractDocxContent(byte[] content) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(content))) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
            return extractor.getText();
        }
    }


    /**
     * Index document for search
     */
    private void indexDocument(Document document) {
        try {
            com.jivs.platform.service.search.IndexRequest indexRequest = new com.jivs.platform.service.search.IndexRequest();
            indexRequest.setId(String.valueOf(document.getId()));
            indexRequest.setEntityType(com.jivs.platform.service.search.EntityType.DOCUMENT);

            Map<String, Object> data = new HashMap<>();
            data.put("title", document.getTitle());
            data.put("filename", document.getFilename());
            data.put("description", document.getDescription());
            data.put("content", document.getContent());
            data.put("fileType", document.getFileType());
            data.put("tags", document.getTags());
            data.put("size", document.getSize());
            data.put("createdDate", document.getCreatedDate());

            indexRequest.setData(data);
            searchService.indexDocument(indexRequest);

        } catch (Exception e) {
            log.error("Error indexing document: {}", e.getMessage());
        }
    }

    /**
     * Get file type from filename
     */
    private String getFileType(String filename) {
        if (filename == null) return "unknown";

        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return "unknown";
    }

    /**
     * Count words in text
     */
    private int countWords(String text) {
        if (text == null || text.isEmpty()) return 0;
        return text.split("\\s+").length;
    }

    /**
     * Convert Document entity to DTO
     */
    private DocumentDTO toDTO(Document document) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(document.getId());
        dto.setFilename(document.getFilename());
        dto.setTitle(document.getTitle());
        dto.setDescription(document.getDescription());
        dto.setFileType(document.getFileType());
        dto.setSize(document.getSize());
        dto.setTags(document.getTags());
        dto.setStatus(document.getStatus());
        dto.setArchived(document.isArchived());
        dto.setStorageTier(document.getStorageTier());
        dto.setCreatedDate(document.getCreatedDate());
        dto.setModifiedDate(document.getModifiedDate());
        dto.setChecksum(document.getChecksum());
        return dto;
    }
}