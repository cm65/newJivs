package com.jivs.platform.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility to test BCrypt password hash
 */
public class BCryptTest {

    @Test
    public void testBCryptHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "password";
        String hashFromMigration = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

        System.out.println("Testing BCrypt hash from migration...");
        System.out.println("Raw password: " + rawPassword);
        System.out.println("Hash from migration: " + hashFromMigration);

        boolean matches = encoder.matches(rawPassword, hashFromMigration);
        System.out.println("Hash matches 'password': " + matches);

        // Generate a new hash for comparison
        String newHash = encoder.encode(rawPassword);
        System.out.println("\nFresh BCrypt hash for 'password': " + newHash);

        assert matches : "The hash from migration should match 'password'";
    }
}
