package com.jivs.platform.service.views;

import com.jivs.platform.common.exception.BusinessException;
import com.jivs.platform.common.exception.ResourceNotFoundException;
import com.jivs.platform.domain.SavedView;
import com.jivs.platform.dto.SavedViewDTO;
import com.jivs.platform.repository.SavedViewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ViewsService
 * Part of Sprint 2 - Workflow 7: Advanced Filtering Implementation
 */
@ExtendWith(MockitoExtension.class)
class ViewsServiceTest {

    @Mock
    private SavedViewRepository repository;

    @InjectMocks
    private ViewsService service;

    private SavedView testView;
    private SavedViewDTO testViewDTO;
    private Long testUserId;
    private String testModule;
    private String testViewName;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testModule = "extractions";
        testViewName = "My Test View";

        Map<String, Object> filters = new HashMap<>();
        filters.put("status", "RUNNING");

        Map<String, String> sorting = new HashMap<>();
        sorting.put("createdAt", "DESC");

        List<String> visibleColumns = Arrays.asList("name", "status", "recordsExtracted");

        testView = SavedView.builder()
                .id(1L)
                .userId(testUserId)
                .module(testModule)
                .viewName(testViewName)
                .isDefault(false)
                .filters(filters)
                .sorting(sorting)
                .visibleColumns(visibleColumns)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testViewDTO = SavedViewDTO.builder()
                .id(1L)
                .module(testModule)
                .viewName(testViewName)
                .isDefault(false)
                .filters(filters)
                .sorting(sorting)
                .visibleColumns(visibleColumns)
                .build();
    }

    @Test
    void getViews_WhenViewsExist_ReturnsListOfViews() {
        // Arrange
        List<SavedView> views = Arrays.asList(testView);
        when(repository.findByUserIdAndModule(testUserId, testModule)).thenReturn(views);

        // Act
        List<SavedViewDTO> result = service.getViews(testUserId, testModule);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testViewName, result.get(0).getViewName());
        assertEquals(testModule, result.get(0).getModule());
        verify(repository, times(1)).findByUserIdAndModule(testUserId, testModule);
    }

    @Test
    void getViews_WhenNoViewsExist_ReturnsEmptyList() {
        // Arrange
        when(repository.findByUserIdAndModule(testUserId, testModule)).thenReturn(Collections.emptyList());

        // Act
        List<SavedViewDTO> result = service.getViews(testUserId, testModule);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findByUserIdAndModule(testUserId, testModule);
    }

    @Test
    void getView_WhenViewExists_ReturnsView() {
        // Arrange
        when(repository.findByUserIdAndModuleAndViewName(testUserId, testModule, testViewName))
                .thenReturn(Optional.of(testView));

        // Act
        SavedViewDTO result = service.getView(testUserId, testModule, testViewName);

        // Assert
        assertNotNull(result);
        assertEquals(testViewName, result.getViewName());
        assertEquals(testModule, result.getModule());
        assertFalse(result.getIsDefault());
        verify(repository, times(1)).findByUserIdAndModuleAndViewName(testUserId, testModule, testViewName);
    }

    @Test
    void getView_WhenViewNotExists_ThrowsResourceNotFoundException() {
        // Arrange
        when(repository.findByUserIdAndModuleAndViewName(testUserId, testModule, testViewName))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            service.getView(testUserId, testModule, testViewName);
        });
        verify(repository, times(1)).findByUserIdAndModuleAndViewName(testUserId, testModule, testViewName);
    }

    @Test
    void getDefaultView_WhenDefaultExists_ReturnsDefaultView() {
        // Arrange
        testView.setAsDefault();
        when(repository.findByUserIdAndModuleAndIsDefaultTrue(testUserId, testModule))
                .thenReturn(Optional.of(testView));

        // Act
        SavedViewDTO result = service.getDefaultView(testUserId, testModule);

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsDefault());
        assertEquals(testViewName, result.getViewName());
        verify(repository, times(1)).findByUserIdAndModuleAndIsDefaultTrue(testUserId, testModule);
    }

    @Test
    void getDefaultView_WhenNoDefaultExists_ReturnsNull() {
        // Arrange
        when(repository.findByUserIdAndModuleAndIsDefaultTrue(testUserId, testModule))
                .thenReturn(Optional.empty());

        // Act
        SavedViewDTO result = service.getDefaultView(testUserId, testModule);

        // Assert
        assertNull(result);
        verify(repository, times(1)).findByUserIdAndModuleAndIsDefaultTrue(testUserId, testModule);
    }

    @Test
    void createView_WhenValidData_CreatesView() {
        // Arrange
        when(repository.existsByUserIdAndModuleAndViewName(testUserId, testModule, testViewName))
                .thenReturn(false);
        when(repository.save(any(SavedView.class))).thenReturn(testView);

        // Act
        SavedViewDTO result = service.createView(testUserId, testViewDTO);

        // Assert
        assertNotNull(result);
        assertEquals(testViewName, result.getViewName());
        assertEquals(testModule, result.getModule());
        verify(repository, times(1)).existsByUserIdAndModuleAndViewName(testUserId, testModule, testViewName);
        verify(repository, times(1)).save(any(SavedView.class));
        verify(repository, never()).unsetDefaultViews(anyLong(), anyString());
    }

    @Test
    void createView_WhenDuplicateName_ThrowsBusinessException() {
        // Arrange
        when(repository.existsByUserIdAndModuleAndViewName(testUserId, testModule, testViewName))
                .thenReturn(true);

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            service.createView(testUserId, testViewDTO);
        });
        verify(repository, times(1)).existsByUserIdAndModuleAndViewName(testUserId, testModule, testViewName);
        verify(repository, never()).save(any(SavedView.class));
    }

    @Test
    void createView_WhenSetAsDefault_UnsetsOtherDefaults() {
        // Arrange
        testViewDTO.setIsDefault(true);
        when(repository.existsByUserIdAndModuleAndViewName(testUserId, testModule, testViewName))
                .thenReturn(false);
        when(repository.save(any(SavedView.class))).thenReturn(testView);
        doNothing().when(repository).unsetDefaultViews(testUserId, testModule);

        // Act
        SavedViewDTO result = service.createView(testUserId, testViewDTO);

        // Assert
        assertNotNull(result);
        verify(repository, times(1)).unsetDefaultViews(testUserId, testModule);
        verify(repository, times(1)).save(any(SavedView.class));
    }

    @Test
    void updateView_WhenValidData_UpdatesView() {
        // Arrange
        SavedViewDTO updateDTO = SavedViewDTO.builder()
                .filters(Collections.singletonMap("status", "COMPLETED"))
                .build();

        when(repository.findByUserIdAndModuleAndViewName(testUserId, testModule, testViewName))
                .thenReturn(Optional.of(testView));
        when(repository.save(any(SavedView.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        SavedViewDTO result = service.updateView(testUserId, testModule, testViewName, updateDTO);

        // Assert
        assertNotNull(result);
        verify(repository, times(1)).findByUserIdAndModuleAndViewName(testUserId, testModule, testViewName);
        verify(repository, times(1)).save(any(SavedView.class));
    }

    @Test
    void updateView_WhenViewNotExists_ThrowsResourceNotFoundException() {
        // Arrange
        SavedViewDTO updateDTO = SavedViewDTO.builder()
                .filters(Collections.singletonMap("status", "COMPLETED"))
                .build();

        when(repository.findByUserIdAndModuleAndViewName(testUserId, testModule, testViewName))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            service.updateView(testUserId, testModule, testViewName, updateDTO);
        });
        verify(repository, times(1)).findByUserIdAndModuleAndViewName(testUserId, testModule, testViewName);
        verify(repository, never()).save(any(SavedView.class));
    }

    @Test
    void updateView_WhenChangingNameToDuplicate_ThrowsBusinessException() {
        // Arrange
        String newName = "Duplicate View";
        SavedViewDTO updateDTO = SavedViewDTO.builder()
                .viewName(newName)
                .build();

        when(repository.findByUserIdAndModuleAndViewName(testUserId, testModule, testViewName))
                .thenReturn(Optional.of(testView));
        when(repository.existsByUserIdAndModuleAndViewName(testUserId, testModule, newName))
                .thenReturn(true);

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            service.updateView(testUserId, testModule, testViewName, updateDTO);
        });
        verify(repository, times(1)).findByUserIdAndModuleAndViewName(testUserId, testModule, testViewName);
        verify(repository, times(1)).existsByUserIdAndModuleAndViewName(testUserId, testModule, newName);
        verify(repository, never()).save(any(SavedView.class));
    }

    @Test
    void updateView_WhenSetAsDefault_UnsetsOtherDefaults() {
        // Arrange
        SavedViewDTO updateDTO = SavedViewDTO.builder()
                .isDefault(true)
                .build();

        when(repository.findByUserIdAndModuleAndViewName(testUserId, testModule, testViewName))
                .thenReturn(Optional.of(testView));
        when(repository.save(any(SavedView.class))).thenAnswer(i -> i.getArguments()[0]);
        doNothing().when(repository).unsetDefaultViews(testUserId, testModule);

        // Act
        SavedViewDTO result = service.updateView(testUserId, testModule, testViewName, updateDTO);

        // Assert
        assertNotNull(result);
        verify(repository, times(1)).unsetDefaultViews(testUserId, testModule);
        verify(repository, times(1)).save(any(SavedView.class));
    }

    @Test
    void deleteView_WhenViewExists_DeletesView() {
        // Arrange
        when(repository.existsByUserIdAndModuleAndViewName(testUserId, testModule, testViewName))
                .thenReturn(true);
        doNothing().when(repository).deleteByUserIdAndModuleAndViewName(testUserId, testModule, testViewName);

        // Act
        service.deleteView(testUserId, testModule, testViewName);

        // Assert
        verify(repository, times(1)).existsByUserIdAndModuleAndViewName(testUserId, testModule, testViewName);
        verify(repository, times(1)).deleteByUserIdAndModuleAndViewName(testUserId, testModule, testViewName);
    }

    @Test
    void deleteView_WhenViewNotExists_ThrowsResourceNotFoundException() {
        // Arrange
        when(repository.existsByUserIdAndModuleAndViewName(testUserId, testModule, testViewName))
                .thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            service.deleteView(testUserId, testModule, testViewName);
        });
        verify(repository, times(1)).existsByUserIdAndModuleAndViewName(testUserId, testModule, testViewName);
        verify(repository, never()).deleteByUserIdAndModuleAndViewName(anyLong(), anyString(), anyString());
    }

    @Test
    void setDefaultView_WhenViewExists_SetsAsDefault() {
        // Arrange
        when(repository.findByUserIdAndModuleAndViewName(testUserId, testModule, testViewName))
                .thenReturn(Optional.of(testView));
        when(repository.save(any(SavedView.class))).thenAnswer(i -> i.getArguments()[0]);
        doNothing().when(repository).unsetDefaultViews(testUserId, testModule);

        // Act
        SavedViewDTO result = service.setDefaultView(testUserId, testModule, testViewName);

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsDefault());
        verify(repository, times(1)).findByUserIdAndModuleAndViewName(testUserId, testModule, testViewName);
        verify(repository, times(1)).unsetDefaultViews(testUserId, testModule);
        verify(repository, times(1)).save(any(SavedView.class));
    }

    @Test
    void setDefaultView_WhenViewNotExists_ThrowsResourceNotFoundException() {
        // Arrange
        when(repository.findByUserIdAndModuleAndViewName(testUserId, testModule, testViewName))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            service.setDefaultView(testUserId, testModule, testViewName);
        });
        verify(repository, times(1)).findByUserIdAndModuleAndViewName(testUserId, testModule, testViewName);
        verify(repository, never()).unsetDefaultViews(anyLong(), anyString());
        verify(repository, never()).save(any(SavedView.class));
    }

    @Test
    void getViewCount_ReturnsCorrectCount() {
        // Arrange
        long expectedCount = 3L;
        when(repository.countByUserIdAndModule(testUserId, testModule)).thenReturn(expectedCount);

        // Act
        long result = service.getViewCount(testUserId, testModule);

        // Assert
        assertEquals(expectedCount, result);
        verify(repository, times(1)).countByUserIdAndModule(testUserId, testModule);
    }
}
