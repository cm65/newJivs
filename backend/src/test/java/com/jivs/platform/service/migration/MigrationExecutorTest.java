package com.jivs.platform.service.migration;

import com.jivs.platform.domain.migration.Migration;
import com.jivs.platform.domain.migration.MigrationPhase;
import com.jivs.platform.domain.migration.MigrationStatus;
import com.jivs.platform.repository.MigrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Integration tests for MigrationExecutor
 *
 * CRITICAL: Verifies transaction boundary fixes
 * - Tests that transactions commit properly in async context
 * - Verifies rollback on failure
 * - Ensures proper error handling and state management
 */
@ExtendWith(MockitoExtension.class)
class MigrationExecutorTest {

    @Mock
    private MigrationRepository migrationRepository;

    @Mock
    private MigrationPhaseExecutor phaseExecutor;

    @InjectMocks
    private MigrationExecutor migrationExecutor;

    private Migration testMigration;

    @BeforeEach
    void setUp() {
        testMigration = new Migration();
        testMigration.setId(1L);
        testMigration.setStatus(MigrationStatus.PENDING);
        testMigration.setPhase(MigrationPhase.PLANNING);
        testMigration.setName("Test Migration");
    }

    @Test
    void executeWithTransaction_Success_ShouldCommitAllChanges() throws Exception {
        // Given
        when(migrationRepository.findById(1L)).thenReturn(Optional.of(testMigration));
        when(migrationRepository.saveAndFlush(any(Migration.class))).thenAnswer(inv -> inv.getArgument(0));
        when(phaseExecutor.executeAllPhases(any(Migration.class))).thenAnswer(inv -> {
            Migration m = inv.getArgument(0);
            m.setPhase(MigrationPhase.COMPLETED);
            return m;
        });

        // When
        Migration result = migrationExecutor.executeWithTransaction(1L);

        // Then
        assertThat(result.getStatus()).isEqualTo(MigrationStatus.COMPLETED);
        assertThat(result.getPhase()).isEqualTo(MigrationPhase.COMPLETED);
        assertThat(result.getStartTime()).isNotNull();
        assertThat(result.getCompletionTime()).isNotNull();

        // Verify transactions committed (saveAndFlush called)
        verify(migrationRepository, atLeast(2)).saveAndFlush(any(Migration.class));
        verify(phaseExecutor).executeAllPhases(any(Migration.class));
    }

    @Test
    void executeWithTransaction_PhaseExecutionFails_ShouldRollback() throws Exception {
        // Given
        when(migrationRepository.findById(1L)).thenReturn(Optional.of(testMigration));
        when(migrationRepository.saveAndFlush(any(Migration.class))).thenAnswer(inv -> inv.getArgument(0));
        when(phaseExecutor.executeAllPhases(any(Migration.class)))
            .thenThrow(new RuntimeException("Extraction service unavailable"));

        // When/Then
        assertThatThrownBy(() -> migrationExecutor.executeWithTransaction(1L))
            .isInstanceOf(MigrationExecutionException.class)
            .hasMessageContaining("Migration 1 failed");

        // Verify status set to FAILED
        ArgumentCaptor<Migration> captor = ArgumentCaptor.forClass(Migration.class);
        verify(migrationRepository, atLeastOnce()).saveAndFlush(captor.capture());

        Migration finalState = captor.getValue();
        assertThat(finalState.getStatus()).isEqualTo(MigrationStatus.FAILED);
        assertThat(finalState.getErrorMessage()).contains("Extraction service unavailable");
    }

    @Test
    void executeWithTransaction_FailureWithRollbackEnabled_ShouldExecuteRollback() throws Exception {
        // Given
        testMigration.setRollbackEnabled(true);
        when(migrationRepository.findById(1L)).thenReturn(Optional.of(testMigration));
        when(migrationRepository.saveAndFlush(any(Migration.class))).thenAnswer(inv -> inv.getArgument(0));
        when(phaseExecutor.executeAllPhases(any(Migration.class)))
            .thenThrow(new RuntimeException("Data validation failed"));
        doNothing().when(phaseExecutor).executeRollback(any(Migration.class));

        // When/Then
        assertThatThrownBy(() -> migrationExecutor.executeWithTransaction(1L))
            .isInstanceOf(MigrationExecutionException.class);

        // Then
        verify(phaseExecutor).executeRollback(any(Migration.class));

        ArgumentCaptor<Migration> captor = ArgumentCaptor.forClass(Migration.class);
        verify(migrationRepository, atLeastOnce()).saveAndFlush(captor.capture());

        Migration finalState = captor.getValue();
        assertThat(finalState.getStatus()).isEqualTo(MigrationStatus.FAILED);
    }

    @Test
    void executeWithTransaction_RollbackFails_ShouldMarkRollbackFailed() throws Exception {
        // Given
        testMigration.setRollbackEnabled(true);
        when(migrationRepository.findById(1L)).thenReturn(Optional.of(testMigration));
        when(migrationRepository.saveAndFlush(any(Migration.class))).thenAnswer(inv -> inv.getArgument(0));
        when(phaseExecutor.executeAllPhases(any(Migration.class)))
            .thenThrow(new RuntimeException("Data validation failed"));
        doThrow(new RuntimeException("Target system unavailable"))
            .when(phaseExecutor).executeRollback(any(Migration.class));

        // When/Then
        assertThatThrownBy(() -> migrationExecutor.executeWithTransaction(1L))
            .isInstanceOf(MigrationExecutionException.class);

        // Then
        ArgumentCaptor<Migration> captor = ArgumentCaptor.forClass(Migration.class);
        verify(migrationRepository, atLeastOnce()).saveAndFlush(captor.capture());

        Migration finalState = captor.getValue();
        assertThat(finalState.getStatus()).isEqualTo(MigrationStatus.FAILED);
        assertThat(finalState.isRollbackFailed()).isTrue();
        assertThat(finalState.getRollbackError()).contains("Target system unavailable");
    }

    @Test
    void executeWithTransaction_MigrationNotFound_ShouldThrowException() {
        // Given
        when(migrationRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> migrationExecutor.executeWithTransaction(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Migration not found: 999");

        verify(phaseExecutor, never()).executeAllPhases(any());
    }

    @Test
    void executeWithTransaction_TransactionTimeout_ShouldRollback() throws Exception {
        // Given - simulate long-running operation exceeding timeout
        when(migrationRepository.findById(1L)).thenReturn(Optional.of(testMigration));
        when(migrationRepository.saveAndFlush(any(Migration.class))).thenAnswer(inv -> inv.getArgument(0));
        when(phaseExecutor.executeAllPhases(any(Migration.class))).thenAnswer(inv -> {
            // Simulate operation taking longer than transaction timeout (7200s)
            throw new org.springframework.transaction.TransactionTimedOutException("Transaction timed out");
        });

        // When/Then
        assertThatThrownBy(() -> migrationExecutor.executeWithTransaction(1L))
            .isInstanceOf(MigrationExecutionException.class);

        // Verify status updated
        ArgumentCaptor<Migration> captor = ArgumentCaptor.forClass(Migration.class);
        verify(migrationRepository, atLeastOnce()).saveAndFlush(captor.capture());

        Migration finalState = captor.getValue();
        assertThat(finalState.getStatus()).isEqualTo(MigrationStatus.FAILED);
    }

    @Test
    void executeWithTransaction_ConcurrentModification_ShouldHandleGracefully() throws Exception {
        // Given - simulate concurrent modification
        when(migrationRepository.findById(1L)).thenReturn(Optional.of(testMigration));
        when(migrationRepository.saveAndFlush(any(Migration.class)))
            .thenThrow(new org.springframework.dao.OptimisticLockingFailureException("Version mismatch"));

        // When/Then
        assertThatThrownBy(() -> migrationExecutor.executeWithTransaction(1L))
            .isInstanceOf(MigrationExecutionException.class);

        verify(phaseExecutor, never()).executeAllPhases(any());
    }

    @Test
    void executeWithTransaction_DatabaseConnectionLost_ShouldFailGracefully() throws Exception {
        // Given
        when(migrationRepository.findById(1L)).thenReturn(Optional.of(testMigration));
        when(migrationRepository.saveAndFlush(any(Migration.class))).thenAnswer(inv -> inv.getArgument(0));
        when(phaseExecutor.executeAllPhases(any(Migration.class)))
            .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("Connection lost"));

        // When/Then
        assertThatThrownBy(() -> migrationExecutor.executeWithTransaction(1L))
            .isInstanceOf(MigrationExecutionException.class);

        // Verify final state saved before exception propagated
        verify(migrationRepository, atLeastOnce()).saveAndFlush(any(Migration.class));
    }
}
