package com.jivs.platform.repository;

import com.jivs.platform.domain.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for UserPreferences entity.
 * Part of Sprint 2 - Workflow 6: Dark Mode Implementation
 */
@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {

    /**
     * Find preferences by user ID
     * @param userId User ID
     * @return Optional containing user preferences if found
     */
    Optional<UserPreferences> findByUserId(Long userId);

    /**
     * Check if preferences exist for user
     * @param userId User ID
     * @return true if preferences exist
     */
    boolean existsByUserId(Long userId);

    /**
     * Delete preferences by user ID
     * @param userId User ID
     */
    void deleteByUserId(Long userId);
}
