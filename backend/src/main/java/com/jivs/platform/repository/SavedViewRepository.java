package com.jivs.platform.repository;

import com.jivs.platform.domain.SavedView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SavedView entity.
 * Part of Sprint 2 - Workflow 7: Advanced Filtering Implementation
 */
@Repository
public interface SavedViewRepository extends JpaRepository<SavedView, Long> {

    /**
     * Find all views for a specific user and module
     * @param userId User ID
     * @param module Module name (extractions, migrations, data-quality, compliance)
     * @return List of saved views
     */
    List<SavedView> findByUserIdAndModule(Long userId, String module);

    /**
     * Find all views for a specific user
     * @param userId User ID
     * @return List of saved views
     */
    List<SavedView> findByUserId(Long userId);

    /**
     * Find a specific view by name for a user and module
     * @param userId User ID
     * @param module Module name
     * @param viewName View name
     * @return Optional containing the saved view if found
     */
    Optional<SavedView> findByUserIdAndModuleAndViewName(Long userId, String module, String viewName);

    /**
     * Find the default view for a user and module
     * @param userId User ID
     * @param module Module name
     * @return Optional containing the default view if exists
     */
    Optional<SavedView> findByUserIdAndModuleAndIsDefaultTrue(Long userId, String module);

    /**
     * Check if a view exists for user, module, and name
     * @param userId User ID
     * @param module Module name
     * @param viewName View name
     * @return true if view exists
     */
    boolean existsByUserIdAndModuleAndViewName(Long userId, String module, String viewName);

    /**
     * Delete a specific view
     * @param userId User ID
     * @param module Module name
     * @param viewName View name
     */
    void deleteByUserIdAndModuleAndViewName(Long userId, String module, String viewName);

    /**
     * Delete all views for a user
     * @param userId User ID
     */
    void deleteByUserId(Long userId);

    /**
     * Unset all default views for a user and module
     * Used before setting a new default view
     * @param userId User ID
     * @param module Module name
     */
    @Modifying
    @Query("UPDATE SavedView v SET v.isDefault = false WHERE v.userId = :userId AND v.module = :module AND v.isDefault = true")
    void unsetDefaultViews(@Param("userId") Long userId, @Param("module") String module);

    /**
     * Count views for a user and module
     * @param userId User ID
     * @param module Module name
     * @return Count of views
     */
    long countByUserIdAndModule(Long userId, String module);
}
