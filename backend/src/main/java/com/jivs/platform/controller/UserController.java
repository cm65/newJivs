package com.jivs.platform.controller;

import com.jivs.platform.dto.UserDTO;
import com.jivs.platform.domain.user.User;
import com.jivs.platform.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for user management operations
 * Handles CRUD operations for users and role assignments
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management operations")
public class UserController {

    private final UserService userService;

    /**
     * Get all users with pagination
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users", description = "Get all users with pagination (Admin only)")
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @Parameter(description = "Pagination parameters") Pageable pageable) {

        log.info("Fetching all users with pagination");

        Page<User> users = userService.getAllUsers(pageable);
        Page<UserDTO> userDTOs = users.map(UserDTO::fromEntity);

        return ResponseEntity.ok(userDTOs);
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Get user by ID", description = "Get a specific user by ID")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        log.info("Fetching user with ID: {}", id);

        User user = userService.findById(id);
        UserDTO userDTO = UserDTO.fromEntity(user);

        return ResponseEntity.ok(userDTO);
    }

    /**
     * Update user profile
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Update user", description = "Update user profile information")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {

        log.info("Updating user with ID: {}", id);

        String firstName = (String) updates.get("firstName");
        String lastName = (String) updates.get("lastName");
        String phone = (String) updates.get("phone");

        User updatedUser = userService.updateUserProfile(id, firstName, lastName, phone);
        UserDTO userDTO = UserDTO.fromEntity(updatedUser);

        return ResponseEntity.ok(userDTO);
    }

    /**
     * Delete user
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Delete a user (Admin only)")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        log.info("Deleting user with ID: {}", id);

        userService.deleteUser(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "User deleted successfully");
        response.put("userId", id);

        return ResponseEntity.ok(response);
    }

    /**
     * Enable or disable user account
     */
    @PutMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enable/disable user", description = "Enable or disable a user account (Admin only)")
    public ResponseEntity<UserDTO> setUserEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled) {

        log.info("{} user with ID: {}", enabled ? "Enabling" : "Disabling", id);

        User user = userService.setUserEnabled(id, enabled);
        UserDTO userDTO = UserDTO.fromEntity(user);

        return ResponseEntity.ok(userDTO);
    }

    /**
     * Assign role to user
     */
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign role to user", description = "Assign a role to a user (Admin only)")
    public ResponseEntity<UserDTO> assignRole(
            @PathVariable Long id,
            @RequestParam String roleName) {

        log.info("Assigning role {} to user with ID: {}", roleName, id);

        User user = userService.assignRole(id, roleName);
        UserDTO userDTO = UserDTO.fromEntity(user);

        return ResponseEntity.ok(userDTO);
    }

    /**
     * Remove role from user
     */
    @DeleteMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove role from user", description = "Remove a role from a user (Admin only)")
    public ResponseEntity<UserDTO> removeRole(
            @PathVariable Long id,
            @RequestParam String roleName) {

        log.info("Removing role {} from user with ID: {}", roleName, id);

        User user = userService.removeRole(id, roleName);
        UserDTO userDTO = UserDTO.fromEntity(user);

        return ResponseEntity.ok(userDTO);
    }

    /**
     * Change user password
     */
    @PutMapping("/{id}/password")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Change password", description = "Change user password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> passwordData) {

        log.info("Changing password for user with ID: {}", id);

        String currentPassword = passwordData.get("currentPassword");
        String newPassword = passwordData.get("newPassword");

        userService.changePassword(id, currentPassword, newPassword);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Password changed successfully");

        return ResponseEntity.ok(response);
    }
}
