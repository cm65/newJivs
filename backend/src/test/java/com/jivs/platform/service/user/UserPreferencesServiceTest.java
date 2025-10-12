package com.jivs.platform.service.user;

import com.jivs.platform.domain.UserPreferences;
import com.jivs.platform.dto.UserPreferencesDTO;
import com.jivs.platform.repository.UserPreferencesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserPreferencesService
 */
@ExtendWith(MockitoExtension.class)
class UserPreferencesServiceTest {

    @Mock
    private UserPreferencesRepository repository;

    @InjectMocks
    private UserPreferencesService service;

    private UserPreferences testPreferences;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testPreferences = UserPreferences.builder()
                .id(1L)
                .userId(testUserId)
                .theme("light")
                .language("en")
                .notificationsEnabled(true)
                .emailNotifications(true)
                .build();
    }

    @Test
    void getUserPreferences_WhenExists_ReturnsPreferences() {
        // Arrange
        when(repository.findByUserId(testUserId)).thenReturn(Optional.of(testPreferences));

        // Act
        UserPreferences result = service.getUserPreferences(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals("light", result.getTheme());
        verify(repository, times(1)).findByUserId(testUserId);
    }

    @Test
    void getUserPreferences_WhenNotExists_CreatesDefault() {
        // Arrange
        when(repository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(repository.save(any(UserPreferences.class))).thenReturn(testPreferences);

        // Act
        UserPreferences result = service.getUserPreferences(testUserId);

        // Assert
        assertNotNull(result);
        verify(repository, times(1)).findByUserId(testUserId);
        verify(repository, times(1)).save(any(UserPreferences.class));
    }

    @Test
    void updateTheme_UpdatesSuccessfully() {
        // Arrange
        String newTheme = "dark";
        when(repository.findByUserId(testUserId)).thenReturn(Optional.of(testPreferences));
        when(repository.save(any(UserPreferences.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        UserPreferences result = service.updateTheme(testUserId, newTheme);

        // Assert
        assertNotNull(result);
        assertEquals(newTheme, result.getTheme());
        verify(repository, times(1)).save(any(UserPreferences.class));
    }

    @Test
    void updateTheme_WhenPreferencesNotExist_CreatesAndUpdates() {
        // Arrange
        String newTheme = "dark";
        when(repository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(repository.save(any(UserPreferences.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        UserPreferences result = service.updateTheme(testUserId, newTheme);

        // Assert
        assertNotNull(result);
        assertEquals(newTheme, result.getTheme());
        // Called twice: once in createDefaultPreferences(), once in updateTheme()
        verify(repository, times(2)).save(any(UserPreferences.class));
    }

    @Test
    void updatePreferences_UpdatesAllFields() {
        // Arrange
        UserPreferencesDTO dto = UserPreferencesDTO.builder()
                .theme("dark")
                .language("es")
                .notificationsEnabled(false)
                .emailNotifications(false)
                .build();

        when(repository.findByUserId(testUserId)).thenReturn(Optional.of(testPreferences));
        when(repository.save(any(UserPreferences.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        UserPreferences result = service.updatePreferences(testUserId, dto);

        // Assert
        assertNotNull(result);
        assertEquals("dark", result.getTheme());
        assertEquals("es", result.getLanguage());
        assertFalse(result.isNotificationsEnabled());
        assertFalse(result.isEmailNotifications());
        verify(repository, times(1)).save(any(UserPreferences.class));
    }

    @Test
    void updatePreferences_PartialUpdate_UpdatesOnlyProvidedFields() {
        // Arrange
        UserPreferencesDTO dto = UserPreferencesDTO.builder()
                .theme("dark")
                // Other fields null
                .build();

        when(repository.findByUserId(testUserId)).thenReturn(Optional.of(testPreferences));
        when(repository.save(any(UserPreferences.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        UserPreferences result = service.updatePreferences(testUserId, dto);

        // Assert
        assertNotNull(result);
        assertEquals("dark", result.getTheme());
        assertEquals("en", result.getLanguage()); // Should remain unchanged
        assertTrue(result.isNotificationsEnabled()); // Should remain unchanged
        verify(repository, times(1)).save(any(UserPreferences.class));
    }

    @Test
    void deleteUserPreferences_DeletesSuccessfully() {
        // Arrange
        doNothing().when(repository).deleteByUserId(testUserId);

        // Act
        service.deleteUserPreferences(testUserId);

        // Assert
        verify(repository, times(1)).deleteByUserId(testUserId);
    }

    @Test
    void hasPreferences_WhenExists_ReturnsTrue() {
        // Arrange
        when(repository.existsByUserId(testUserId)).thenReturn(true);

        // Act
        boolean result = service.hasPreferences(testUserId);

        // Assert
        assertTrue(result);
        verify(repository, times(1)).existsByUserId(testUserId);
    }

    @Test
    void hasPreferences_WhenNotExists_ReturnsFalse() {
        // Arrange
        when(repository.existsByUserId(testUserId)).thenReturn(false);

        // Act
        boolean result = service.hasPreferences(testUserId);

        // Assert
        assertFalse(result);
        verify(repository, times(1)).existsByUserId(testUserId);
    }

    @Test
    void createDefaultPreferences_CreatesWithCorrectDefaults() {
        // Arrange
        when(repository.save(any(UserPreferences.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        UserPreferences result = service.createDefaultPreferences(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals("light", result.getTheme());
        assertEquals("en", result.getLanguage());
        assertTrue(result.isNotificationsEnabled());
        assertTrue(result.isEmailNotifications());
        verify(repository, times(1)).save(any(UserPreferences.class));
    }
}
