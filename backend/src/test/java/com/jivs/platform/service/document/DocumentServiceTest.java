package com.jivs.platform.service.document;

import com.jivs.platform.domain.Document;
import com.jivs.platform.dto.DocumentDTO;
import com.jivs.platform.dto.DocumentSearchRequest;
import com.jivs.platform.dto.DocumentSearchResponse;
import com.jivs.platform.repository.DocumentRepository;
import com.jivs.platform.service.DocumentService;
import com.jivs.platform.service.search.SearchService;
import com.jivs.platform.service.search.SearchResponse;
import com.jivs.platform.service.search.SearchResult;
import com.jivs.platform.service.storage.FileData;
import com.jivs.platform.service.storage.StorageService;
import com.jivs.platform.service.storage.StorageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DocumentService
 * Tests document upload, search, extraction, and deletion
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentService Tests")
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private SearchService searchService;

    @InjectMocks
    private DocumentService documentService;

    private Document sampleDocument;
    private MultipartFile mockFile;
    private StorageResult mockStorageResult;

    @BeforeEach
    void setUp() {
        // Create sample document
        sampleDocument = new Document();
        sampleDocument.setId("doc-123");
        sampleDocument.setFilename("test.pdf");
        sampleDocument.setTitle("Test Document");
        sampleDocument.setDescription("Test description");
        sampleDocument.setFileType("pdf");
        sampleDocument.setSize(1024L);
        sampleDocument.setStatus("ACTIVE");
        sampleDocument.setArchived(false);
        sampleDocument.setCreatedDate(new Date());
        sampleDocument.setModifiedDate(new Date());
        sampleDocument.setContent("Sample content for testing");
        sampleDocument.setChecksum("abc123");
        sampleDocument.setStoragePath("/data/storage/documents/doc-123");

        // Create mock file
        mockFile = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "Test content".getBytes()
        );

        // Create mock storage result
        mockStorageResult = new StorageResult();
        mockStorageResult.setStorageId("storage-123");
        mockStorageResult.setPath("/data/storage/documents/storage-123");
        mockStorageResult.setChecksum("abc123");
    }

    @Test
    @DisplayName("Should upload document successfully")
    void shouldUploadDocumentSuccessfully() throws IOException {
        // Given
        when(storageService.storeFile(anyString(), any(byte[].class), any()))
            .thenReturn(mockStorageResult);
        when(documentRepository.save(any(Document.class)))
            .thenReturn(sampleDocument);

        // When
        DocumentDTO result = documentService.uploadDocument(
            mockFile,
            "Test Document",
            "Test description",
            Arrays.asList("tag1", "tag2")
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFilename()).isEqualTo("test.pdf");
        assertThat(result.getTitle()).isEqualTo("Test Document");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");

        verify(documentRepository).save(any(Document.class));
        verify(storageService).storeFile(anyString(), any(byte[].class), any());
        verify(searchService).indexDocument(any());
    }

    @Test
    @DisplayName("Should handle upload with null title")
    void shouldHandleUploadWithNullTitle() throws IOException {
        // Given
        when(storageService.storeFile(anyString(), any(byte[].class), any()))
            .thenReturn(mockStorageResult);
        when(documentRepository.save(any(Document.class)))
            .thenReturn(sampleDocument);

        // When
        DocumentDTO result = documentService.uploadDocument(
            mockFile,
            null, // Null title
            "Test description",
            null
        );

        // Then
        assertThat(result).isNotNull();
        // Should use filename as title when title is null
    }

    @Test
    @DisplayName("Should search documents successfully")
    void shouldSearchDocumentsSuccessfully() {
        // Given
        DocumentSearchRequest request = new DocumentSearchRequest();
        request.setQuery("test");
        request.setFrom(0);
        request.setSize(10);

        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setTotalHits(1L);

        SearchResult searchResult = new SearchResult();
        searchResult.setId("doc-123");
        searchResult.setScore(0.95f);
        searchResponse.setResults(Collections.singletonList(searchResult));
        searchResponse.setHighlights(new HashMap<>());
        searchResponse.setFacets(new HashMap<>());

        when(searchService.search(any())).thenReturn(searchResponse);
        when(documentRepository.findById("doc-123"))
            .thenReturn(Optional.of(sampleDocument));

        // When
        DocumentSearchResponse response = documentService.searchDocuments(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getTotalHits()).isEqualTo(1L);
        assertThat(response.getDocuments()).hasSize(1);
        assertThat(response.getDocuments().get(0).getId()).isEqualTo("doc-123");

        verify(searchService).search(any());
    }

    @Test
    @DisplayName("Should handle search failure gracefully")
    void shouldHandleSearchFailureGracefully() {
        // Given
        DocumentSearchRequest request = new DocumentSearchRequest();
        request.setQuery("test");

        when(searchService.search(any()))
            .thenThrow(new RuntimeException("Search service unavailable"));

        // When
        DocumentSearchResponse response = documentService.searchDocuments(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isNotNull();
    }

    @Test
    @DisplayName("Should get all documents with pagination")
    void shouldGetAllDocumentsWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Document> documentPage = new PageImpl<>(
            Collections.singletonList(sampleDocument),
            pageable,
            1
        );

        when(documentRepository.findAll(pageable)).thenReturn(documentPage);

        // When
        Page<DocumentDTO> result = documentService.getAllDocuments(pageable, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo("doc-123");
    }

    @Test
    @DisplayName("Should filter documents by status")
    void shouldFilterDocumentsByStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Document> documentPage = new PageImpl<>(
            Collections.singletonList(sampleDocument),
            pageable,
            1
        );

        when(documentRepository.findByStatus("ACTIVE", pageable))
            .thenReturn(documentPage);

        // When
        Page<DocumentDTO> result = documentService.getAllDocuments(pageable, "ACTIVE", null);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(documentRepository).findByStatus("ACTIVE", pageable);
    }

    @Test
    @DisplayName("Should filter documents by archived status")
    void shouldFilterDocumentsByArchivedStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Document> documentPage = new PageImpl<>(
            Collections.singletonList(sampleDocument),
            pageable,
            1
        );

        when(documentRepository.findByArchived(true, pageable))
            .thenReturn(documentPage);

        // When
        Page<DocumentDTO> result = documentService.getAllDocuments(pageable, null, true);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(documentRepository).findByArchived(true, pageable);
    }

    @Test
    @DisplayName("Should get document by ID")
    void shouldGetDocumentById() {
        // Given
        when(documentRepository.findById("doc-123"))
            .thenReturn(Optional.of(sampleDocument));

        // When
        DocumentDTO result = documentService.getDocument("doc-123");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("doc-123");
        assertThat(result.getFilename()).isEqualTo("test.pdf");
    }

    @Test
    @DisplayName("Should return null for non-existent document")
    void shouldReturnNullForNonExistentDocument() {
        // Given
        when(documentRepository.findById("non-existent"))
            .thenReturn(Optional.empty());

        // When
        DocumentDTO result = documentService.getDocument("non-existent");

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should download document successfully")
    void shouldDownloadDocumentSuccessfully() throws IOException {
        // Given
        FileData fileData = new FileData();
        fileData.setData("Test content".getBytes());
        fileData.setFilename("test.pdf");

        when(documentRepository.findById("doc-123"))
            .thenReturn(Optional.of(sampleDocument));
        when(storageService.retrieveFile(anyString()))
            .thenReturn(fileData);

        // When
        byte[] result = documentService.downloadDocument("doc-123");

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("Test content".getBytes());
    }

    @Test
    @DisplayName("Should return null when downloading non-existent document")
    void shouldReturnNullWhenDownloadingNonExistentDocument() throws IOException {
        // Given
        when(documentRepository.findById("non-existent"))
            .thenReturn(Optional.empty());

        // When
        byte[] result = documentService.downloadDocument("non-existent");

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should restore document successfully")
    void shouldRestoreDocumentSuccessfully() {
        // Given
        sampleDocument.setArchived(true);
        when(documentRepository.findById("doc-123"))
            .thenReturn(Optional.of(sampleDocument));
        when(documentRepository.save(any(Document.class)))
            .thenReturn(sampleDocument);

        // When
        boolean result = documentService.restoreDocument("doc-123");

        // Then
        assertThat(result).isTrue();

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());

        Document saved = captor.getValue();
        assertThat(saved.isArchived()).isFalse();
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Should return false when restoring non-existent document")
    void shouldReturnFalseWhenRestoringNonExistentDocument() {
        // Given
        when(documentRepository.findById("non-existent"))
            .thenReturn(Optional.empty());

        // When
        boolean result = documentService.restoreDocument("non-existent");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should delete document successfully")
    void shouldDeleteDocumentSuccessfully() {
        // Given
        when(documentRepository.findById("doc-123"))
            .thenReturn(Optional.of(sampleDocument));
        doNothing().when(storageService).deleteFile(anyString());
        doNothing().when(documentRepository).delete(any(Document.class));

        // When
        boolean result = documentService.deleteDocument("doc-123");

        // Then
        assertThat(result).isTrue();
        verify(storageService).deleteFile(anyString());
        verify(documentRepository).delete(sampleDocument);
        verify(searchService).deleteFromIndex(eq("doc-123"), any());
    }

    @Test
    @DisplayName("Should handle storage deletion failure gracefully")
    void shouldHandleStorageDeletionFailureGracefully() throws IOException {
        // Given
        when(documentRepository.findById("doc-123"))
            .thenReturn(Optional.of(sampleDocument));
        doThrow(new IOException("Storage unavailable"))
            .when(storageService).deleteFile(anyString());

        // When
        boolean result = documentService.deleteDocument("doc-123");

        // Then
        assertThat(result).isTrue(); // Should still delete from database
        verify(documentRepository).delete(sampleDocument);
    }

    @Test
    @DisplayName("Should update document metadata successfully")
    void shouldUpdateDocumentMetadataSuccessfully() {
        // Given
        DocumentDTO updates = new DocumentDTO();
        updates.setTitle("Updated Title");
        updates.setDescription("Updated description");
        updates.setTags(Arrays.asList("new-tag"));

        when(documentRepository.findById("doc-123"))
            .thenReturn(Optional.of(sampleDocument));
        when(documentRepository.save(any(Document.class)))
            .thenReturn(sampleDocument);

        // When
        DocumentDTO result = documentService.updateDocument("doc-123", updates);

        // Then
        assertThat(result).isNotNull();

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());

        Document saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Updated Title");
        assertThat(saved.getDescription()).isEqualTo("Updated description");
        assertThat(saved.getTags()).contains("new-tag");

        verify(searchService).indexDocument(any());
    }

    @Test
    @DisplayName("Should return null when updating non-existent document")
    void shouldReturnNullWhenUpdatingNonExistentDocument() {
        // Given
        DocumentDTO updates = new DocumentDTO();
        updates.setTitle("Updated Title");

        when(documentRepository.findById("non-existent"))
            .thenReturn(Optional.empty());

        // When
        DocumentDTO result = documentService.updateDocument("non-existent", updates);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should get document statistics")
    void shouldGetDocumentStatistics() {
        // Given
        when(documentRepository.count()).thenReturn(100L);
        when(documentRepository.countByStatus("ACTIVE")).thenReturn(80L);
        when(documentRepository.countByArchived(true)).thenReturn(20L);
        when(documentRepository.sumSizeByFileType("pdf")).thenReturn(1024000L);
        when(documentRepository.countByFileType("pdf")).thenReturn(50L);

        // When
        Map<String, Object> stats = documentService.getStatistics();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.get("totalDocuments")).isEqualTo(100L);
        assertThat(stats.get("activeDocuments")).isEqualTo(80L);
        assertThat(stats.get("archivedDocuments")).isEqualTo(20L);
        assertThat(stats).containsKeys("sizeByType", "countByType");
    }

    @Test
    @DisplayName("Should extract content from document")
    void shouldExtractContentFromDocument() {
        // Given
        when(documentRepository.findById("doc-123"))
            .thenReturn(Optional.of(sampleDocument));

        // When
        Map<String, Object> content = documentService.extractContent("doc-123");

        // Then
        assertThat(content).isNotNull();
        assertThat(content.get("id")).isEqualTo("doc-123");
        assertThat(content.get("filename")).isEqualTo("test.pdf");
        assertThat(content.get("content")).isEqualTo("Sample content for testing");
        assertThat(content.get("wordCount")).isNotNull();
    }

    @Test
    @DisplayName("Should return null when extracting content from non-existent document")
    void shouldReturnNullWhenExtractingContentFromNonExistentDocument() {
        // Given
        when(documentRepository.findById("non-existent"))
            .thenReturn(Optional.empty());

        // When
        Map<String, Object> content = documentService.extractContent("non-existent");

        // Then
        assertThat(content).isNull();
    }

    @Test
    @DisplayName("Should calculate word count correctly")
    void shouldCalculateWordCountCorrectly() {
        // Given
        sampleDocument.setContent("This is a test document with exactly ten words here");
        when(documentRepository.findById("doc-123"))
            .thenReturn(Optional.of(sampleDocument));

        // When
        Map<String, Object> content = documentService.extractContent("doc-123");

        // Then
        assertThat(content.get("wordCount")).isEqualTo(10);
    }

    @Test
    @DisplayName("Should handle empty content when calculating word count")
    void shouldHandleEmptyContentWhenCalculatingWordCount() {
        // Given
        sampleDocument.setContent("");
        when(documentRepository.findById("doc-123"))
            .thenReturn(Optional.of(sampleDocument));

        // When
        Map<String, Object> content = documentService.extractContent("doc-123");

        // Then
        assertThat(content.get("wordCount")).isEqualTo(0);
    }

    @Test
    @DisplayName("Should convert document to DTO correctly")
    void shouldConvertDocumentToDtoCorrectly() {
        // Given
        when(documentRepository.findById("doc-123"))
            .thenReturn(Optional.of(sampleDocument));

        // When
        DocumentDTO dto = documentService.getDocument("doc-123");

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(sampleDocument.getId());
        assertThat(dto.getFilename()).isEqualTo(sampleDocument.getFilename());
        assertThat(dto.getTitle()).isEqualTo(sampleDocument.getTitle());
        assertThat(dto.getDescription()).isEqualTo(sampleDocument.getDescription());
        assertThat(dto.getFileType()).isEqualTo(sampleDocument.getFileType());
        assertThat(dto.getSize()).isEqualTo(sampleDocument.getSize());
        assertThat(dto.getStatus()).isEqualTo(sampleDocument.getStatus());
        assertThat(dto.isArchived()).isEqualTo(sampleDocument.isArchived());
        assertThat(dto.getChecksum()).isEqualTo(sampleDocument.getChecksum());
    }
}
