package com.jivs.platform.service.extraction;

import com.jivs.platform.domain.Extraction;
import com.jivs.platform.domain.ExtractionConfig;
import com.jivs.platform.repository.ExtractionRepository;
import com.jivs.platform.repository.ExtractionConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExtractionServiceTest {

    @Mock
    private ExtractionRepository extractionRepository;

    @Mock
    private ExtractionConfigRepository configRepository;

    @Mock
    private JdbcConnector jdbcConnector;

    @Mock
    private SapConnector sapConnector;

    @Mock
    private FileConnector fileConnector;

    @Mock
    private ApiConnector apiConnector;

    @InjectMocks
    private ExtractionService extractionService;

    private Extraction testExtraction;
    private ExtractionConfig testConfig;

    @BeforeEach
    void setUp() {
        testExtraction = new Extraction();
        testExtraction.setId(UUID.randomUUID().toString());
        testExtraction.setName("Test Extraction");
        testExtraction.setSourceType("JDBC");
        testExtraction.setStatus("PENDING");
        testExtraction.setRecordsExtracted(0L);
        testExtraction.setCreatedAt(new Date());
        testExtraction.setUpdatedAt(new Date());

        testConfig = new ExtractionConfig();
        testConfig.setId(UUID.randomUUID().toString());
        testConfig.setExtractionId(testExtraction.getId());
        testConfig.setConnectionString("jdbc:postgresql://localhost:5432/test");
        testConfig.setUsername("test_user");
        testConfig.setPassword("test_pass");
        testConfig.setQuery("SELECT * FROM test_table");
        testConfig.setCreatedAt(new Date());
    }

    @Test
    void testCreateExtraction_Success() {
        // Arrange
        when(extractionRepository.save(any(Extraction.class))).thenReturn(testExtraction);
        when(configRepository.save(any(ExtractionConfig.class))).thenReturn(testConfig);

        // Act
        Extraction result = extractionService.createExtraction(testExtraction, testConfig);

        // Assert
        assertNotNull(result);
        assertEquals("Test Extraction", result.getName());
        assertEquals("PENDING", result.getStatus());
        verify(extractionRepository, times(1)).save(any(Extraction.class));
        verify(configRepository, times(1)).save(any(ExtractionConfig.class));
    }

    @Test
    void testCreateExtraction_NullExtraction_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            extractionService.createExtraction(null, testConfig)
        );
        verify(extractionRepository, never()).save(any(Extraction.class));
    }

    @Test
    void testGetExtraction_Success() {
        // Arrange
        when(extractionRepository.findById(testExtraction.getId()))
            .thenReturn(Optional.of(testExtraction));

        // Act
        Optional<Extraction> result = extractionService.getExtraction(testExtraction.getId());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testExtraction.getId(), result.get().getId());
        verify(extractionRepository, times(1)).findById(testExtraction.getId());
    }

    @Test
    void testGetExtraction_NotFound() {
        // Arrange
        String nonExistentId = UUID.randomUUID().toString();
        when(extractionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act
        Optional<Extraction> result = extractionService.getExtraction(nonExistentId);

        // Assert
        assertFalse(result.isPresent());
        verify(extractionRepository, times(1)).findById(nonExistentId);
    }

    @Test
    void testGetAllExtractions_Success() {
        // Arrange
        List<Extraction> extractions = Arrays.asList(testExtraction);
        Page<Extraction> page = new PageImpl<>(extractions);
        Pageable pageable = PageRequest.of(0, 20);

        when(extractionRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<Extraction> result = extractionService.getAllExtractions(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testExtraction.getId(), result.getContent().get(0).getId());
        verify(extractionRepository, times(1)).findAll(pageable);
    }

    @Test
    void testGetExtractionsByStatus_Success() {
        // Arrange
        List<Extraction> extractions = Arrays.asList(testExtraction);
        Page<Extraction> page = new PageImpl<>(extractions);
        Pageable pageable = PageRequest.of(0, 20);

        when(extractionRepository.findByStatus("PENDING", pageable)).thenReturn(page);

        // Act
        Page<Extraction> result = extractionService.getExtractionsByStatus("PENDING", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("PENDING", result.getContent().get(0).getStatus());
        verify(extractionRepository, times(1)).findByStatus("PENDING", pageable);
    }

    @Test
    void testStartExtraction_Success() {
        // Arrange
        when(extractionRepository.findById(testExtraction.getId()))
            .thenReturn(Optional.of(testExtraction));
        when(configRepository.findByExtractionId(testExtraction.getId()))
            .thenReturn(Optional.of(testConfig));
        when(extractionRepository.save(any(Extraction.class))).thenReturn(testExtraction);

        // Act
        extractionService.startExtraction(testExtraction.getId());

        // Assert
        verify(extractionRepository, times(1)).findById(testExtraction.getId());
        verify(configRepository, times(1)).findByExtractionId(testExtraction.getId());
        // Status should be updated to RUNNING
        verify(extractionRepository, atLeastOnce()).save(any(Extraction.class));
    }

    @Test
    void testStartExtraction_NotFound_ThrowsException() {
        // Arrange
        String nonExistentId = UUID.randomUUID().toString();
        when(extractionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            extractionService.startExtraction(nonExistentId)
        );
        verify(extractionRepository, times(1)).findById(nonExistentId);
    }

    @Test
    void testStartExtraction_AlreadyRunning_ThrowsException() {
        // Arrange
        testExtraction.setStatus("RUNNING");
        when(extractionRepository.findById(testExtraction.getId()))
            .thenReturn(Optional.of(testExtraction));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            extractionService.startExtraction(testExtraction.getId())
        );
    }

    @Test
    void testStopExtraction_Success() {
        // Arrange
        testExtraction.setStatus("RUNNING");
        when(extractionRepository.findById(testExtraction.getId()))
            .thenReturn(Optional.of(testExtraction));
        when(extractionRepository.save(any(Extraction.class))).thenReturn(testExtraction);

        // Act
        extractionService.stopExtraction(testExtraction.getId());

        // Assert
        verify(extractionRepository, times(1)).findById(testExtraction.getId());
        verify(extractionRepository, times(1)).save(any(Extraction.class));
    }

    @Test
    void testStopExtraction_NotRunning_ThrowsException() {
        // Arrange
        testExtraction.setStatus("PENDING");
        when(extractionRepository.findById(testExtraction.getId()))
            .thenReturn(Optional.of(testExtraction));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            extractionService.stopExtraction(testExtraction.getId())
        );
    }

    @Test
    void testDeleteExtraction_Success() {
        // Arrange
        when(extractionRepository.findById(testExtraction.getId()))
            .thenReturn(Optional.of(testExtraction));
        doNothing().when(extractionRepository).deleteById(testExtraction.getId());

        // Act
        extractionService.deleteExtraction(testExtraction.getId());

        // Assert
        verify(extractionRepository, times(1)).findById(testExtraction.getId());
        verify(extractionRepository, times(1)).deleteById(testExtraction.getId());
    }

    @Test
    void testDeleteExtraction_Running_ThrowsException() {
        // Arrange
        testExtraction.setStatus("RUNNING");
        when(extractionRepository.findById(testExtraction.getId()))
            .thenReturn(Optional.of(testExtraction));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            extractionService.deleteExtraction(testExtraction.getId())
        );
        verify(extractionRepository, never()).deleteById(any());
    }

    @Test
    void testGetStatistics_Success() {
        // Arrange
        testExtraction.setRecordsExtracted(1000L);
        testExtraction.setStartTime(new Date(System.currentTimeMillis() - 60000)); // 1 minute ago
        testExtraction.setEndTime(new Date());
        when(extractionRepository.findById(testExtraction.getId()))
            .thenReturn(Optional.of(testExtraction));

        // Act
        Map<String, Object> stats = extractionService.getStatistics(testExtraction.getId());

        // Assert
        assertNotNull(stats);
        assertTrue(stats.containsKey("recordsExtracted"));
        assertEquals(1000L, stats.get("recordsExtracted"));
        verify(extractionRepository, times(1)).findById(testExtraction.getId());
    }

    @Test
    void testGetStatistics_NotFound_ThrowsException() {
        // Arrange
        String nonExistentId = UUID.randomUUID().toString();
        when(extractionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            extractionService.getStatistics(nonExistentId)
        );
    }

    @Test
    void testTestConnection_JDBC_Success() {
        // Arrange
        Map<String, String> config = new HashMap<>();
        config.put("type", "JDBC");
        config.put("connectionString", "jdbc:postgresql://localhost:5432/test");
        config.put("username", "test_user");
        config.put("password", "test_pass");

        when(jdbcConnector.testConnection(any())).thenReturn(true);

        // Act
        boolean result = extractionService.testConnection(config);

        // Assert
        assertTrue(result);
        verify(jdbcConnector, times(1)).testConnection(any());
    }

    @Test
    void testTestConnection_InvalidType_ThrowsException() {
        // Arrange
        Map<String, String> config = new HashMap<>();
        config.put("type", "INVALID");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            extractionService.testConnection(config)
        );
    }

    @Test
    void testUpdateExtractionStatus() {
        // Arrange
        when(extractionRepository.findById(testExtraction.getId()))
            .thenReturn(Optional.of(testExtraction));
        when(extractionRepository.save(any(Extraction.class))).thenReturn(testExtraction);

        // Act
        extractionService.updateExtractionStatus(testExtraction.getId(), "COMPLETED");

        // Assert
        verify(extractionRepository, times(1)).findById(testExtraction.getId());
        verify(extractionRepository, times(1)).save(any(Extraction.class));
    }

    @Test
    void testUpdateRecordsExtracted() {
        // Arrange
        when(extractionRepository.findById(testExtraction.getId()))
            .thenReturn(Optional.of(testExtraction));
        when(extractionRepository.save(any(Extraction.class))).thenReturn(testExtraction);

        // Act
        extractionService.updateRecordsExtracted(testExtraction.getId(), 500L);

        // Assert
        verify(extractionRepository, times(1)).findById(testExtraction.getId());
        verify(extractionRepository, times(1)).save(any(Extraction.class));
    }

    @Test
    void testGetExtractionConfig_Success() {
        // Arrange
        when(configRepository.findByExtractionId(testExtraction.getId()))
            .thenReturn(Optional.of(testConfig));

        // Act
        Optional<ExtractionConfig> result =
            extractionService.getExtractionConfig(testExtraction.getId());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testConfig.getId(), result.get().getId());
        verify(configRepository, times(1)).findByExtractionId(testExtraction.getId());
    }

    @Test
    void testGetRecentExtractions() {
        // Arrange
        List<Extraction> extractions = Arrays.asList(testExtraction);
        when(extractionRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(extractions);

        // Act
        List<Extraction> result = extractionService.getRecentExtractions();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(extractionRepository, times(1)).findTop10ByOrderByCreatedAtDesc();
    }

    @Test
    void testCountByStatus() {
        // Arrange
        when(extractionRepository.countByStatus("PENDING")).thenReturn(5L);

        // Act
        long result = extractionService.countByStatus("PENDING");

        // Assert
        assertEquals(5L, result);
        verify(extractionRepository, times(1)).countByStatus("PENDING");
    }
}
