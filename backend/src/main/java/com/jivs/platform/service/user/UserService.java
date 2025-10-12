package com.jivs.platform.service.user;

import com.jivs.platform.common.exception.BusinessException;
import com.jivs.platform.common.exception.ResourceNotFoundException;
import com.jivs.platform.common.exception.ValidationException;
import com.jivs.platform.domain.user.Role;
import com.jivs.platform.domain.user.User;
import com.jivs.platform.repository.RoleRepository;
import com.jivs.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service for user management operations
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register a new user
     */
    @Transactional
    public User registerUser(String username, String email, String password, String firstName, String lastName) {
        log.info("Registering new user: {}", username);

        // Validate uniqueness
        if (userRepository.existsByUsername(username)) {
            throw new ValidationException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new ValidationException("Email already registered: " + email);
        }

        // Create new user
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setEmailVerificationToken(UUID.randomUUID().toString());

        // Assign default role
        Role defaultRole = roleRepository.findByName("ROLE_BUSINESS_USER")
                .orElseThrow(() -> new BusinessException("Default role not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", username);

        return savedUser;
    }

    /**
     * Find user by ID
     */
    @Cacheable(value = "users", key = "#id")
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    /**
     * Find user by username
     */
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    /**
     * Find user by email
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    /**
     * Get all users with pagination
     */
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Update user profile
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public User updateUserProfile(Long userId, String firstName, String lastName, String phone) {
        User user = findById(userId);

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    /**
     * Change user password
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = findById(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ValidationException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password changed for user: {}", user.getUsername());
    }

    /**
     * Enable/disable user account
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public User setUserEnabled(Long userId, boolean enabled) {
        User user = findById(userId);
        user.setEnabled(enabled);
        user.setUpdatedAt(LocalDateTime.now());

        log.info("User {} {}", user.getUsername(), enabled ? "enabled" : "disabled");
        return userRepository.save(user);
    }

    /**
     * Assign role to user
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public User assignRole(Long userId, String roleName) {
        User user = findById(userId);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));

        user.getRoles().add(role);
        user.setUpdatedAt(LocalDateTime.now());

        log.info("Role {} assigned to user {}", roleName, user.getUsername());
        return userRepository.save(user);
    }

    /**
     * Remove role from user
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public User removeRole(Long userId, String roleName) {
        User user = findById(userId);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));

        user.getRoles().remove(role);
        user.setUpdatedAt(LocalDateTime.now());

        log.info("Role {} removed from user {}", roleName, user.getUsername());
        return userRepository.save(user);
    }

    /**
     * Verify email with token
     */
    @Transactional
    @CacheEvict(value = "users", key = "#result.id")
    public User verifyEmail(String token) {
        User user = userRepository.findAll().stream()
                .filter(u -> token.equals(u.getEmailVerificationToken()))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Invalid verification token"));

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setUpdatedAt(LocalDateTime.now());

        log.info("Email verified for user: {}", user.getUsername());
        return userRepository.save(user);
    }

    /**
     * Update last login time
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void updateLastLogin(Long userId) {
        User user = findById(userId);
        user.setLastLoginAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
    }

    /**
     * Handle failed login attempt
     */
    @Transactional
    public void handleFailedLogin(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

            // Lock account after 5 failed attempts
            if (user.getFailedLoginAttempts() >= 5) {
                user.setAccountNonLocked(false);
                user.setLockedUntil(LocalDateTime.now().plusHours(1));
                log.warn("Account locked for user {} due to failed login attempts", username);
            }

            userRepository.save(user);
        }
    }

    /**
     * Delete user
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void deleteUser(Long userId) {
        User user = findById(userId);
        log.info("Deleting user: {}", user.getUsername());
        userRepository.delete(user);
    }
}