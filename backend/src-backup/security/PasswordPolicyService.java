package com.jivs.platform.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Service for enforcing password policies and account security
 * Implements NIST 800-63B password guidelines
 */
@Service
@Slf4j
public class PasswordPolicyService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Password policy configuration
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final int PASSWORD_HISTORY_SIZE = 5; // Remember last 5 passwords
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 30;
    private static final long PASSWORD_EXPIRY_DAYS = 90;

    // Common weak passwords (subset - in production, use a comprehensive list)
    private static final List<String> COMMON_PASSWORDS = List.of(
        "password", "Password123", "123456", "qwerty", "admin",
        "welcome", "letmein", "monkey", "dragon", "master",
        "sunshine", "password1", "123456789", "12345678", "12345"
    );

    // Regex patterns for password complexity
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");
    private static final Pattern REPEATING_CHARS = Pattern.compile("(.)\\1{2,}"); // 3+ repeating chars
    private static final Pattern SEQUENTIAL_CHARS = Pattern.compile("(abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|pqr|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz|012|123|234|345|456|567|678|789)");

    /**
     * Validate password against all policy rules
     *
     * @param password The password to validate
     * @param username The username (to prevent username in password)
     * @param userId The user ID (for password history check)
     * @return Validation result with success flag and error messages
     */
    public PasswordValidationResult validatePassword(String password, String username, String userId) {
        PasswordValidationResult result = new PasswordValidationResult();
        List<String> errors = new ArrayList<>();

        // 1. Check null or empty
        if (password == null || password.isEmpty()) {
            errors.add("Password cannot be empty");
            result.setValid(false);
            result.setErrors(errors);
            return result;
        }

        // 2. Check length
        if (password.length() < MIN_PASSWORD_LENGTH) {
            errors.add(String.format("Password must be at least %d characters long", MIN_PASSWORD_LENGTH));
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            errors.add(String.format("Password must not exceed %d characters", MAX_PASSWORD_LENGTH));
        }

        // 3. Check complexity - must have at least 3 of 4 character types
        int complexityScore = 0;
        if (UPPERCASE_PATTERN.matcher(password).find()) complexityScore++;
        if (LOWERCASE_PATTERN.matcher(password).find()) complexityScore++;
        if (DIGIT_PATTERN.matcher(password).find()) complexityScore++;
        if (SPECIAL_CHAR_PATTERN.matcher(password).find()) complexityScore++;

        if (complexityScore < 3) {
            errors.add("Password must contain at least 3 of the following: uppercase letters, lowercase letters, numbers, special characters");
        }

        // 4. Check for common/weak passwords
        String lowerPassword = password.toLowerCase();
        if (COMMON_PASSWORDS.stream().anyMatch(weak -> weak.equalsIgnoreCase(password))) {
            errors.add("Password is too common and easily guessable");
        }

        // 5. Check for username in password
        if (username != null && lowerPassword.contains(username.toLowerCase())) {
            errors.add("Password cannot contain username");
        }

        // 6. Check for repeating characters (e.g., "aaa")
        if (REPEATING_CHARS.matcher(lowerPassword).find()) {
            errors.add("Password cannot contain 3 or more repeating characters");
        }

        // 7. Check for sequential characters (e.g., "abc", "123")
        if (SEQUENTIAL_CHARS.matcher(lowerPassword).find()) {
            errors.add("Password cannot contain sequential characters");
        }

        // 8. Check password history (if userId provided)
        if (userId != null && isPasswordInHistory(userId, password)) {
            errors.add(String.format("Password cannot be one of your last %d passwords", PASSWORD_HISTORY_SIZE));
        }

        result.setValid(errors.isEmpty());
        result.setErrors(errors);
        return result;
    }

    /**
     * Check if password was used recently
     */
    private boolean isPasswordInHistory(String userId, String password) {
        try {
            String key = "password:history:" + userId;
            List<String> passwordHistory = redisTemplate.opsForList().range(key, 0, PASSWORD_HISTORY_SIZE - 1);

            if (passwordHistory != null) {
                for (String historicalHash : passwordHistory) {
                    if (passwordEncoder.matches(password, historicalHash)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Error checking password history for user: {}", userId, e);
            return false; // Fail open - don't block user if Redis is down
        }
    }

    /**
     * Add password to user's history
     */
    public void addToPasswordHistory(String userId, String passwordHash) {
        try {
            String key = "password:history:" + userId;

            // Add to front of list
            redisTemplate.opsForList().leftPush(key, passwordHash);

            // Trim to keep only last N passwords
            redisTemplate.opsForList().trim(key, 0, PASSWORD_HISTORY_SIZE - 1);

            // Set expiration (keep history for 2 years)
            redisTemplate.expire(key, Duration.ofDays(730));

            log.debug("Added password to history for user: {}", userId);
        } catch (Exception e) {
            log.error("Error adding password to history for user: {}", userId, e);
        }
    }

    /**
     * Record failed login attempt
     *
     * @param username The username attempting to log in
     * @return Number of failed attempts
     */
    public int recordFailedLogin(String username) {
        try {
            String key = "login:failed:" + username;
            Long attempts = redisTemplate.opsForValue().increment(key);

            if (attempts == 1) {
                // Set expiration on first attempt
                redisTemplate.expire(key, Duration.ofMinutes(LOCKOUT_DURATION_MINUTES));
            }

            log.warn("Failed login attempt for user: {} - Attempt #{}", username, attempts);

            if (attempts >= MAX_LOGIN_ATTEMPTS) {
                lockAccount(username);
            }

            return attempts.intValue();
        } catch (Exception e) {
            log.error("Error recording failed login for user: {}", username, e);
            return 0;
        }
    }

    /**
     * Reset failed login attempts (called on successful login)
     */
    public void resetFailedLoginAttempts(String username) {
        try {
            String key = "login:failed:" + username;
            redisTemplate.delete(key);
            log.debug("Reset failed login attempts for user: {}", username);
        } catch (Exception e) {
            log.error("Error resetting failed login attempts for user: {}", username, e);
        }
    }

    /**
     * Check if account is locked
     */
    public boolean isAccountLocked(String username) {
        try {
            String key = "account:locked:" + username;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Error checking account lock status for user: {}", username, e);
            return false; // Fail open
        }
    }

    /**
     * Lock account due to excessive failed attempts
     */
    private void lockAccount(String username) {
        try {
            String key = "account:locked:" + username;
            redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()));
            redisTemplate.expire(key, Duration.ofMinutes(LOCKOUT_DURATION_MINUTES));

            log.warn("Account locked due to excessive failed login attempts: {}", username);
        } catch (Exception e) {
            log.error("Error locking account for user: {}", username, e);
        }
    }

    /**
     * Unlock account (admin action)
     */
    public void unlockAccount(String username) {
        try {
            redisTemplate.delete("account:locked:" + username);
            redisTemplate.delete("login:failed:" + username);
            log.info("Account manually unlocked: {}", username);
        } catch (Exception e) {
            log.error("Error unlocking account for user: {}", username, e);
        }
    }

    /**
     * Get remaining lockout time in seconds
     */
    public long getRemainingLockoutTime(String username) {
        try {
            String key = "account:locked:" + username;
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (Exception e) {
            log.error("Error getting lockout time for user: {}", username, e);
            return 0;
        }
    }

    /**
     * Get number of failed login attempts
     */
    public int getFailedLoginAttempts(String username) {
        try {
            String key = "login:failed:" + username;
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Integer.parseInt(value) : 0;
        } catch (Exception e) {
            log.error("Error getting failed login attempts for user: {}", username, e);
            return 0;
        }
    }

    /**
     * Generate a strong random password
     */
    public String generateStrongPassword() {
        // In production, use a proper password generator library
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*()_+-=[]{}";

        StringBuilder password = new StringBuilder();
        java.security.SecureRandom random = new java.security.SecureRandom();

        // Ensure at least one of each character type
        password.append(uppercase.charAt(random.nextInt(uppercase.length())));
        password.append(lowercase.charAt(random.nextInt(lowercase.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));

        // Fill rest with random characters
        String allChars = uppercase + lowercase + digits + special;
        for (int i = 4; i < 16; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle the password
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }

        return new String(passwordArray);
    }

    /**
     * Password validation result
     */
    public static class PasswordValidationResult {
        private boolean valid;
        private List<String> errors = new ArrayList<>();

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }
    }
}
