package com.jivs.platform.controller;

import com.jivs.platform.dto.SavedViewDTO;
import com.jivs.platform.security.UserPrincipal;
import com.jivs.platform.service.views.ViewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API controller for saved filter views
 * Part of Sprint 2 - Workflow 7: Advanced Filtering Implementation
 */
@RestController
@RequestMapping("/api/v1/views")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Saved Views", description = "Saved views management API")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ViewsController {

    private final ViewsService viewsService;

    /**
     * Get all saved views for a module
     *
     * @param module Module name (extractions, migrations, data-quality, compliance)
     * @return List of saved views
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENGINEER', 'VIEWER')")
    @Operation(summary = "Get all views", description = "Get all saved views for a specific module")
    public ResponseEntity<List<SavedViewDTO>> getViews(
            @RequestParam(required = true) String module) {
        Long userId = getCurrentUserId();
        log.debug("Getting saved views for user {} in module {}", userId, module);

        List<SavedViewDTO> views = viewsService.getViews(userId, module);
        return ResponseEntity.ok(views);
    }

    /**
     * Get a specific saved view by name
     *
     * @param module Module name
     * @param viewName View name
     * @return Saved view
     */
    @GetMapping("/{viewName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENGINEER', 'VIEWER')")
    @Operation(summary = "Get view by name", description = "Get a specific saved view by name")
    public ResponseEntity<SavedViewDTO> getView(
            @PathVariable String viewName,
            @RequestParam(required = true) String module) {
        Long userId = getCurrentUserId();
        log.debug("Getting view '{}' for user {} in module {}", viewName, userId, module);

        SavedViewDTO view = viewsService.getView(userId, module, viewName);
        return ResponseEntity.ok(view);
    }

    /**
     * Get the default view for a module
     *
     * @param module Module name
     * @return Default saved view or 404 if not set
     */
    @GetMapping("/default")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENGINEER', 'VIEWER')")
    @Operation(summary = "Get default view", description = "Get the default view for a module")
    public ResponseEntity<SavedViewDTO> getDefaultView(
            @RequestParam(required = true) String module) {
        Long userId = getCurrentUserId();
        log.debug("Getting default view for user {} in module {}", userId, module);

        SavedViewDTO view = viewsService.getDefaultView(userId, module);
        if (view == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(view);
    }

    /**
     * Create a new saved view
     *
     * @param dto Saved view DTO
     * @return Created view
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENGINEER')")
    @Operation(summary = "Create view", description = "Create a new saved view")
    public ResponseEntity<SavedViewDTO> createView(
            @Valid @RequestBody SavedViewDTO dto) {
        Long userId = getCurrentUserId();
        log.info("Creating view '{}' for user {} in module {}", dto.getViewName(), userId, dto.getModule());

        SavedViewDTO createdView = viewsService.createView(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdView);
    }

    /**
     * Update an existing saved view
     *
     * @param viewName View name
     * @param module Module name
     * @param dto Updated view data
     * @return Updated view
     */
    @PutMapping("/{viewName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENGINEER')")
    @Operation(summary = "Update view", description = "Update an existing saved view")
    public ResponseEntity<SavedViewDTO> updateView(
            @PathVariable String viewName,
            @RequestParam(required = true) String module,
            @Valid @RequestBody SavedViewDTO dto) {
        Long userId = getCurrentUserId();
        log.info("Updating view '{}' for user {} in module {}", viewName, userId, module);

        SavedViewDTO updatedView = viewsService.updateView(userId, module, viewName, dto);
        return ResponseEntity.ok(updatedView);
    }

    /**
     * Delete a saved view
     *
     * @param viewName View name
     * @param module Module name
     * @return Success message
     */
    @DeleteMapping("/{viewName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENGINEER')")
    @Operation(summary = "Delete view", description = "Delete a saved view")
    public ResponseEntity<Map<String, String>> deleteView(
            @PathVariable String viewName,
            @RequestParam(required = true) String module) {
        Long userId = getCurrentUserId();
        log.info("Deleting view '{}' for user {} in module {}", viewName, userId, module);

        viewsService.deleteView(userId, module, viewName);
        return ResponseEntity.ok(Map.of("message", "View deleted successfully"));
    }

    /**
     * Set a view as the default for a module
     *
     * @param viewName View name
     * @param module Module name
     * @return Updated view
     */
    @PostMapping("/{viewName}/set-default")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENGINEER')")
    @Operation(summary = "Set default view", description = "Set a view as the default for a module")
    public ResponseEntity<SavedViewDTO> setDefaultView(
            @PathVariable String viewName,
            @RequestParam(required = true) String module) {
        Long userId = getCurrentUserId();
        log.info("Setting view '{}' as default for user {} in module {}", viewName, userId, module);

        SavedViewDTO view = viewsService.setDefaultView(userId, module, viewName);
        return ResponseEntity.ok(view);
    }

    /**
     * Get view count for a module
     *
     * @param module Module name
     * @return View count
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENGINEER', 'VIEWER')")
    @Operation(summary = "Get view count", description = "Get the count of saved views for a module")
    public ResponseEntity<Map<String, Long>> getViewCount(
            @RequestParam(required = true) String module) {
        Long userId = getCurrentUserId();
        log.debug("Getting view count for user {} in module {}", userId, module);

        long count = viewsService.getViewCount(userId, module);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Get current authenticated user's ID from security context
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userPrincipal.getId();
    }
}
