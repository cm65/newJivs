package com.jivs.platform.service.migration;

import com.jivs.platform.domain.Migration;
import com.jivs.platform.domain.MigrationPhase;
import com.jivs.platform.repository.MigrationRepository;
import com.jivs.platform.repository.MigrationPhaseRepository;
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
class MigrationServiceTest {

    @Mock
    private MigrationRepository migrationRepository;

    @Mock
    private MigrationPhaseRepository phaseRepository;

    @InjectMocks
    private MigrationService migrationService;

    private Migration testMigration;
    private MigrationPhase testPhase;

    @BeforeEach
    void setUp() {
        testMigration = new Migration();
        testMigration.setId(UUID.randomUUID().toString());
        testMigration.setName("Test Migration");
        testMigration.setStatus("PENDING");
        testMigration.setCurrentPhase("PLANNING");
        testMigration.setProgress(0.0);
        testMigration.setRecordsMigrated(0L);
        testMigration.setTotalRecords(1000L);
        testMigration.setCreatedAt(new Date());
        testMigration.setUpdatedAt(new Date());

        testPhase = new MigrationPhase();
        testPhase.setId(UUID.randomUUID().toString());
        testPhase.setMigrationId(testMigration.getId());
        testPhase.setPhaseName("PLANNING");
        testPhase.setStatus("PENDING");
        testPhase.setStartTime(null);
        testPhase.setEndTime(null);
        testPhase.setCreatedAt(new Date());
    }

    @Test
    void testCreateMigration_Success() {
        // Arrange
        when(migrationRepository.save(any(Migration.class))).thenReturn(testMigration);
        when(phaseRepository.saveAll(anyList())).thenReturn(Arrays.asList(testPhase));

        // Act
        Migration result = migrationService.createMigration(testMigration);

        // Assert
        assertNotNull(result);
        assertEquals("Test Migration", result.getName());
        assertEquals("PENDING", result.getStatus());
        verify(migrationRepository, times(1)).save(any(Migration.class));
        verify(phaseRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testCreateMigration_NullMigration_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            migrationService.createMigration(null)
        );
        verify(migrationRepository, never()).save(any(Migration.class));
    }

    @Test
    void testGetMigration_Success() {
        // Arrange
        when(migrationRepository.findById(testMigration.getId()))
            .thenReturn(Optional.of(testMigration));

        // Act
        Optional<Migration> result = migrationService.getMigration(testMigration.getId());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testMigration.getId(), result.get().getId());
        verify(migrationRepository, times(1)).findById(testMigration.getId());
    }

    @Test
    void testGetMigration_NotFound() {
        // Arrange
        String nonExistentId = UUID.randomUUID().toString();
        when(migrationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act
        Optional<Migration> result = migrationService.getMigration(nonExistentId);

        // Assert
        assertFalse(result.isPresent());
        verify(migrationRepository, times(1)).findById(nonExistentId);
    }

    @Test
    void testGetAllMigrations_Success() {
        // Arrange
        List<Migration> migrations = Arrays.asList(testMigration);
        Page<Migration> page = new PageImpl<>(migrations);
        Pageable pageable = PageRequest.of(0, 20);

        when(migrationRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<Migration> result = migrationService.getAllMigrations(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testMigration.getId(), result.getContent().get(0).getId());
        verify(migrationRepository, times(1)).findAll(pageable);
    }

    @Test
    void testGetMigrationsByStatus_Success() {
        // Arrange
        List<Migration> migrations = Arrays.asList(testMigration);
        Page<Migration> page = new PageImpl<>(migrations);
        Pageable pageable = PageRequest.of(0, 20);

        when(migrationRepository.findByStatus("PENDING", pageable)).thenReturn(page);

        // Act
        Page<Migration> result = migrationService.getMigrationsByStatus("PENDING", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("PENDING", result.getContent().get(0).getStatus());
        verify(migrationRepository, times(1)).findByStatus("PENDING", pageable);
    }

    @Test
    void testStartMigration_Success() {
        // Arrange
        when(migrationRepository.findById(testMigration.getId()))
            .thenReturn(Optional.of(testMigration));
        when(migrationRepository.save(any(Migration.class))).thenReturn(testMigration);
        when(phaseRepository.findByMigrationIdAndPhaseName(testMigration.getId(), "PLANNING"))
            .thenReturn(Optional.of(testPhase));
        when(phaseRepository.save(any(MigrationPhase.class))).thenReturn(testPhase);

        // Act
        migrationService.startMigration(testMigration.getId());

        // Assert
        verify(migrationRepository, times(1)).findById(testMigration.getId());
        verify(migrationRepository, atLeastOnce()).save(any(Migration.class));
    }

    @Test
    void testStartMigration_NotFound_ThrowsException() {
        // Arrange
        String nonExistentId = UUID.randomUUID().toString();
        when(migrationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            migrationService.startMigration(nonExistentId)
        );
        verify(migrationRepository, times(1)).findById(nonExistentId);
    }

    @Test
    void testStartMigration_AlreadyRunning_ThrowsException() {
        // Arrange
        testMigration.setStatus("RUNNING");
        when(migrationRepository.findById(testMigration.getId()))
            .thenReturn(Optional.of(testMigration));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            migrationService.startMigration(testMigration.getId())
        );
    }

    @Test
    void testPauseMigration_Success() {
        // Arrange
        testMigration.setStatus("RUNNING");
        when(migrationRepository.findById(testMigration.getId()))
            .thenReturn(Optional.of(testMigration));
        when(migrationRepository.save(any(Migration.class))).thenReturn(testMigration);

        // Act
        migrationService.pauseMigration(testMigration.getId());

        // Assert
        verify(migrationRepository, times(1)).findById(testMigration.getId());
        verify(migrationRepository, times(1)).save(any(Migration.class));
    }

    @Test
    void testPauseMigration_NotRunning_ThrowsException() {
        // Arrange
        testMigration.setStatus("PENDING");
        when(migrationRepository.findById(testMigration.getId()))
            .thenReturn(Optional.of(testMigration));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            migrationService.pauseMigration(testMigration.getId())
        );
    }

    @Test
    void testResumeMigration_Success() {
        // Arrange
        testMigration.setStatus("PAUSED");
        testMigration.setCurrentPhase("EXTRACTION");
        when(migrationRepository.findById(testMigration.getId()))
            .thenReturn(Optional.of(testMigration));
        when(migrationRepository.save(any(Migration.class))).thenReturn(testMigration);

        // Act
        migrationService.resumeMigration(testMigration.getId());

        // Assert
        verify(migrationRepository, times(1)).findById(testMigration.getId());
        verify(migrationRepository, atLeastOnce()).save(any(Migration.class));
    }

    @Test
    void testResumeMigration_NotPaused_ThrowsException() {
        // Arrange
        testMigration.setStatus("RUNNING");
        when(migrationRepository.findById(testMigration.getId()))
            .thenReturn(Optional.of(testMigration));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            migrationService.resumeMigration(testMigration.getId())
        );
    }

    @Test
    void testRollbackMigration_Success() {
        // Arrange
        testMigration.setStatus("COMPLETED");
        when(migrationRepository.findById(testMigration.getId()))
            .thenReturn(Optional.of(testMigration));
        when(migrationRepository.save(any(Migration.class))).thenReturn(testMigration);

        // Act
        migrationService.rollbackMigration(testMigration.getId());

        // Assert
        verify(migrationRepository, times(1)).findById(testMigration.getId());
        verify(migrationRepository, atLeastOnce()).save(any(Migration.class));
    }

    @Test
    void testRollbackMigration_CannotRollback_ThrowsException() {
        // Arrange
        testMigration.setStatus("RUNNING");
        when(migrationRepository.findById(testMigration.getId()))
            .thenReturn(Optional.of(testMigration));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            migrationService.rollbackMigration(testMigration.getId())
        );
    }

    @Test
    void testDeleteMigration_Success() {
        // Arrange
        when(migrationRepository.findById(testMigration.getId()))
            .thenReturn(Optional.of(testMigration));
        doNothing().when(migrationRepository).deleteById(testMigration.getId());

        // Act
        migrationService.deleteMigration(testMigration.getId());

        // Assert
        verify(migrationRepository, times(1)).findById(testMigration.getId());
        verify(migrationRepository, times(1)).deleteById(testMigration.getId());
    }

    @Test
    void testDeleteMigration_Running_ThrowsException() {
        // Arrange
        testMigration.setStatus("RUNNING");
        when(migrationRepository.findById(testMigration.getId()))
            .thenReturn(Optional.of(testMigration));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            migrationService.deleteMigration(testMigration.getId())
        );
        verify(migrationRepository, never()).deleteById(any());
    }

    @Test
    void testGetProgress_Success() {
        // Arrange
        testMigration.setProgress(50.0);
        testMigration.setRecordsMigrated(500L);
        testMigration.setTotalRecords(1000L);
        when(migrationRepository.findById(testMigration.getId()))
            .thenReturn(Optional.of(testMigration));

        // Act
        Map<String, Object> progress = migrationService.getProgress(testMigration.getId());

        // Assert
        assertNotNull(progress);
        assertTrue(progress.containsKey("progress"));
        assertEquals(50.0, progress.get("progress"));
        verify(migrationRepository, times(1)).findById(testMigration.getId());
    }

    @Test
    void testGetProgress_NotFound_ThrowsException() {
        // Arrange
        String nonExistentId = UUID.randomUUID().toString();
        when(migrationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            migrationService.getProgress(nonExistentId)
        );
    }

    @Test
    void testGetStatistics_Success() {
        // Arrange
        testMigration.setRecordsMigrated(1000L);
        testMigration.setTotalRecords(1000L);
        testMigration.setStartTime(new Date(System.currentTimeMillis() - 300000)); // 5 minutes ago
        testMigration.setEndTime(new Date());
        when(migrationRepository.findById(testMigration.getId()))
            .thenReturn(Optional.of(testMigration));

        // Act
        Map<String, Object> stats = migrationService.getStatistics(testMigration.getId());

        // Assert
        assertNotNull(stats);
        assertTrue(stats.containsKey("recordsMigrated"));
        assertEquals(1000L, stats.get("recordsMigrated"));
        verify(migrationRepository, times(1)).findById(testMigration.getId());
    }

    @Test
    void testValidateMigration_Success() {
        // Arrange
        Map<String, Object> config = new HashMap<>();
        config.put("name", "Test Migration");
        config.put("sourceConfig", new HashMap<>());
        config.put("targetConfig", new HashMap<>());

        // Act
        Map<String, Object> result = migrationService.validateMigration(config);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("valid"));
    }

    @Test
    void testValidateMigration_MissingName_Invalid() {
        // Arrange
        Map<String, Object> config = new HashMap<>();
        config.put("sourceConfig", new HashMap<>());
        config.put("targetConfig", new HashMap<>());

        // Act
        Map<String, Object> result = migrationService.validateMigration(config);

        // Assert
        assertNotNull(result);
        assertEquals(false, result.get("valid"));
    }

    @Test
    void testUpdateMigrationStatus() {
        // Arrange
        when(migrationRepository.findById(testMigration.getId()))
            .thenReturn(Optional.of(testMigration));
        when(migrationRepository.save(any(Migration.class))).thenReturn(testMigration);

        // Act
        migrationService.updateMigrationStatus(testMigration.getId(), "RUNNING");

        // Assert
        verify(migrationRepository, times(1)).findById(testMigration.getId());
        verify(migrationRepository, times(1)).save(any(Migration.class));
    }

    @Test
    void testUpdateProgress() {
        // Arrange
        when(migrationRepository.findById(testMigration.getId()))
            .thenReturn(Optional.of(testMigration));
        when(migrationRepository.save(any(Migration.class))).thenReturn(testMigration);

        // Act
        migrationService.updateProgress(testMigration.getId(), 75.0, 750L);

        // Assert
        verify(migrationRepository, times(1)).findById(testMigration.getId());
        verify(migrationRepository, times(1)).save(any(Migration.class));
    }

    @Test
    void testGetPhases_Success() {
        // Arrange
        List<MigrationPhase> phases = Arrays.asList(testPhase);
        when(phaseRepository.findByMigrationIdOrderByPhaseOrder(testMigration.getId()))
            .thenReturn(phases);

        // Act
        List<MigrationPhase> result = migrationService.getPhases(testMigration.getId());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(phaseRepository, times(1)).findByMigrationIdOrderByPhaseOrder(testMigration.getId());
    }

    @Test
    void testUpdatePhaseStatus() {
        // Arrange
        when(phaseRepository.findByMigrationIdAndPhaseName(testMigration.getId(), "PLANNING"))
            .thenReturn(Optional.of(testPhase));
        when(phaseRepository.save(any(MigrationPhase.class))).thenReturn(testPhase);

        // Act
        migrationService.updatePhaseStatus(testMigration.getId(), "PLANNING", "COMPLETED");

        // Assert
        verify(phaseRepository, times(1))
            .findByMigrationIdAndPhaseName(testMigration.getId(), "PLANNING");
        verify(phaseRepository, times(1)).save(any(MigrationPhase.class));
    }

    @Test
    void testGetRecentMigrations() {
        // Arrange
        List<Migration> migrations = Arrays.asList(testMigration);
        when(migrationRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(migrations);

        // Act
        List<Migration> result = migrationService.getRecentMigrations();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(migrationRepository, times(1)).findTop10ByOrderByCreatedAtDesc();
    }

    @Test
    void testCountByStatus() {
        // Arrange
        when(migrationRepository.countByStatus("RUNNING")).thenReturn(3L);

        // Act
        long result = migrationService.countByStatus("RUNNING");

        // Assert
        assertEquals(3L, result);
        verify(migrationRepository, times(1)).countByStatus("RUNNING");
    }

    @Test
    void testGetPhaseOrder() {
        // Test that phases are processed in correct order
        String[] expectedPhases = {
            "PLANNING", "VALIDATION", "EXTRACTION", "TRANSFORMATION",
            "LOADING", "VERIFICATION", "CLEANUP"
        };

        List<String> phases = migrationService.getPhaseOrder();

        assertNotNull(phases);
        assertEquals(7, phases.size());
        assertArrayEquals(expectedPhases, phases.toArray());
    }
}
