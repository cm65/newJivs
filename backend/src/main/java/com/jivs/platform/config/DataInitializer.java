package com.jivs.platform.config;

import com.jivs.platform.domain.Role;
import com.jivs.platform.domain.User;
import com.jivs.platform.repository.RoleRepository;
import com.jivs.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

/**
 * Initialize default data on application startup
 * Creates default admin user if it doesn't exist
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Running data initialization...");

        // Check if admin user exists
        Optional<User> existingAdmin = userRepository.findByUsername("admin");

        if (existingAdmin.isPresent()) {
            log.info("Admin user already exists, skipping initialization");
            return;
        }

        log.info("Creating default admin user...");

        // Find admin role
        Optional<Role> adminRole = roleRepository.findByName("ROLE_ADMIN");

        if (adminRole.isEmpty()) {
            log.error("ROLE_ADMIN not found in database. Please ensure migrations have run.");
            return;
        }

        // Create admin user
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@jivs.com");
        admin.setPasswordHash(passwordEncoder.encode("password"));
        admin.setFirstName("System");
        admin.setLastName("Administrator");
        admin.setEnabled(true);
        admin.setAccountNonExpired(true);
        admin.setAccountNonLocked(true);
        admin.setCredentialsNonExpired(true);
        admin.setEmailVerified(true);
        admin.setRoles(Collections.singleton(adminRole.get()));
        admin.setCreatedBy("SYSTEM");
        admin.setUpdatedBy("SYSTEM");

        userRepository.save(admin);

        log.info("Default admin user created successfully");
        log.info("Username: admin");
        log.info("Password: password");
        log.info("Please change the password after first login!");
    }
}
