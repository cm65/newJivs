package com.jivs.platform.service.views;

import com.jivs.platform.common.exception.BusinessException;
import com.jivs.platform.common.exception.ResourceNotFoundException;
import com.jivs.platform.domain.SavedView;
import com.jivs.platform.dto.SavedViewDTO;
import com.jivs.platform.repository.SavedViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing saved views with filters, sorting, and column preferences.
 * Part of Sprint 2 - Workflow 7: Advanced Filtering Implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ViewsService {

    private final SavedViewRepository repository;

    /**
     * Get all views for a specific user and module
     *
     * @param userId User ID
     * @param module Module name
     * @return List of saved views
     */
    @Transactional(readOnly = true)
    public List<SavedViewDTO> getViews(Long userId, String module) {
        log.debug("Getting all views for user {} in module {}", userId, module);

        List<SavedView> views = repository.findByUserIdAndModule(userId, module);

        return views.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific view by name
     *
     * @param userId User ID
     * @param module Module name
     * @param viewName View name
     * @return Saved view DTO
     */
    @Transactional(readOnly = true)
    public SavedViewDTO getView(Long userId, String module, String viewName) {
        log.debug("Getting view '{}' for user {} in module {}", viewName, userId, module);

        SavedView view = repository.findByUserIdAndModuleAndViewName(userId, module, viewName)
                .orElseThrow(() -> new ResourceNotFoundException("SavedView", "viewName", viewName));

        return convertToDTO(view);
    }

    /**
     * Get the default view for a user and module
     *
     * @param userId User ID
     * @param module Module name
     * @return Optional containing the default view if exists
     */
    @Transactional(readOnly = true)
    public SavedViewDTO getDefaultView(Long userId, String module) {
        log.debug("Getting default view for user {} in module {}", userId, module);

        return repository.findByUserIdAndModuleAndIsDefaultTrue(userId, module)
                .map(this::convertToDTO)
                .orElse(null);
    }

    /**
     * Create a new saved view
     *
     * @param userId User ID
     * @param dto Saved view DTO
     * @return Created saved view
     */
    @Transactional
    public SavedViewDTO createView(Long userId, SavedViewDTO dto) {
        log.info("Creating view '{}' for user {} in module {}", dto.getViewName(), userId, dto.getModule());

        // Check if view already exists
        if (repository.existsByUserIdAndModuleAndViewName(userId, dto.getModule(), dto.getViewName())) {
            throw new BusinessException("A view with name '" + dto.getViewName() + "' already exists for this module");
        }

        // If setting as default, unset other defaults
        if (dto.getIsDefault() != null && dto.getIsDefault()) {
            repository.unsetDefaultViews(userId, dto.getModule());
        }

        SavedView view = SavedView.builder()
                .userId(userId)
                .module(dto.getModule())
                .viewName(dto.getViewName())
                .isDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false)
                .filters(dto.getFilters())
                .sorting(dto.getSorting())
                .visibleColumns(dto.getVisibleColumns())
                .build();

        SavedView savedView = repository.save(view);
        log.info("View '{}' created successfully with ID {}", dto.getViewName(), savedView.getId());

        return convertToDTO(savedView);
    }

    /**
     * Update an existing saved view
     *
     * @param userId User ID
     * @param module Module name
     * @param viewName View name
     * @param dto Updated view data
     * @return Updated saved view
     */
    @Transactional
    public SavedViewDTO updateView(Long userId, String module, String viewName, SavedViewDTO dto) {
        log.info("Updating view '{}' for user {} in module {}", viewName, userId, module);

        SavedView view = repository.findByUserIdAndModuleAndViewName(userId, module, viewName)
                .orElseThrow(() -> new ResourceNotFoundException("SavedView", "viewName", viewName));

        // If setting as default, unset other defaults
        if (dto.getIsDefault() != null && dto.getIsDefault() && !view.getIsDefault()) {
            repository.unsetDefaultViews(userId, module);
        }

        // Update fields
        if (dto.getViewName() != null && !dto.getViewName().equals(view.getViewName())) {
            // Check if new name already exists
            if (repository.existsByUserIdAndModuleAndViewName(userId, module, dto.getViewName())) {
                throw new BusinessException("A view with name '" + dto.getViewName() + "' already exists");
            }
            view.setViewName(dto.getViewName());
        }

        if (dto.getIsDefault() != null) {
            view.setIsDefault(dto.getIsDefault());
        }

        if (dto.getFilters() != null) {
            view.setFilters(dto.getFilters());
        }

        if (dto.getSorting() != null) {
            view.setSorting(dto.getSorting());
        }

        if (dto.getVisibleColumns() != null) {
            view.setVisibleColumns(dto.getVisibleColumns());
        }

        SavedView updatedView = repository.save(view);
        log.info("View '{}' updated successfully", viewName);

        return convertToDTO(updatedView);
    }

    /**
     * Delete a saved view
     *
     * @param userId User ID
     * @param module Module name
     * @param viewName View name
     */
    @Transactional
    public void deleteView(Long userId, String module, String viewName) {
        log.info("Deleting view '{}' for user {} in module {}", viewName, userId, module);

        if (!repository.existsByUserIdAndModuleAndViewName(userId, module, viewName)) {
            throw new ResourceNotFoundException("SavedView", "viewName", viewName);
        }

        repository.deleteByUserIdAndModuleAndViewName(userId, module, viewName);
        log.info("View '{}' deleted successfully", viewName);
    }

    /**
     * Set a view as the default for a module
     *
     * @param userId User ID
     * @param module Module name
     * @param viewName View name
     * @return Updated view
     */
    @Transactional
    public SavedViewDTO setDefaultView(Long userId, String module, String viewName) {
        log.info("Setting view '{}' as default for user {} in module {}", viewName, userId, module);

        SavedView view = repository.findByUserIdAndModuleAndViewName(userId, module, viewName)
                .orElseThrow(() -> new ResourceNotFoundException("SavedView", "viewName", viewName));

        // Unset other defaults
        repository.unsetDefaultViews(userId, module);

        // Set this view as default
        view.setAsDefault();
        SavedView updatedView = repository.save(view);

        log.info("View '{}' set as default", viewName);
        return convertToDTO(updatedView);
    }

    /**
     * Get view count for a user and module
     *
     * @param userId User ID
     * @param module Module name
     * @return Count of views
     */
    @Transactional(readOnly = true)
    public long getViewCount(Long userId, String module) {
        return repository.countByUserIdAndModule(userId, module);
    }

    /**
     * Convert entity to DTO
     */
    private SavedViewDTO convertToDTO(SavedView view) {
        return SavedViewDTO.builder()
                .id(view.getId())
                .module(view.getModule())
                .viewName(view.getViewName())
                .isDefault(view.getIsDefault())
                .filters(view.getFilters())
                .sorting(view.getSorting())
                .visibleColumns(view.getVisibleColumns())
                .createdAt(view.getCreatedAt())
                .updatedAt(view.getUpdatedAt())
                .build();
    }
}
