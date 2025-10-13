package com.jivs.platform.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.jivs.platform.domain.User;
import com.jivs.platform.repository.UserRepository;
import com.jivs.platform.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Auth Contract Test - Provider Side (CRITICAL)
 *
 * This test verifies that the backend authentication endpoints
 * satisfy the contracts defined by the frontend.
 *
 * Auth is CRITICAL because:
 * - Every API call requires authentication
 * - Token format must match exactly
 * - Role-based access control must work
 * - Password flows must be secure
 *
 * Coverage: 8/8 Auth endpoints
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("JiVS Backend")
@PactFolder("../frontend/pacts")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class AuthContractTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setup(PactVerificationContext context) {
        // Configure the test to hit our running Spring Boot application
        context.setTarget(new HttpTestTarget("localhost", port));

        // Mock JWT token generation and validation for consistent testing
        when(jwtTokenProvider.generateToken(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP03fq0Vg");

        when(jwtTokenProvider.generateRefreshToken(anyString()))
                .thenReturn("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.refresh");

        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(1L);
        when(jwtTokenProvider.getRolesFromToken(anyString())).thenReturn(Collections.singletonList("ROLE_USER"));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        // This will run once for each interaction in the pact file
        context.verifyInteraction();
    }

    /**
     * Provider States - Set up test data for different authentication scenarios
     */

    @State("user exists with valid credentials")
    public void userWithValidCredentials() {
        System.out.println("Setting up: User exists with valid credentials");

        // Clear existing data
        userRepository.deleteAll();

        // Create test user with encoded password
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser@example.com");
        user.setEmail("testuser@example.com");
        user.setPassword(passwordEncoder.encode("ValidPassword123!"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setActive(true);
        user.setRoles("ROLE_USER");
        user.setCreatedDate(LocalDateTime.now());
        userRepository.save(user);
    }

    @State("system allows registration")
    public void systemAllowsRegistration() {
        System.out.println("Setting up: System allows registration");

        // Clear existing data to allow new registration
        userRepository.deleteAll();
    }

    @State("user with email already exists")
    public void userWithEmailExists() {
        System.out.println("Setting up: User with email already exists");

        // Clear existing data
        userRepository.deleteAll();

        // Create existing user
        User user = new User();
        user.setId(2L);
        user.setUsername("existinguser@example.com");
        user.setEmail("existinguser@example.com");
        user.setPassword(passwordEncoder.encode("ExistingPassword123!"));
        user.setFirstName("Existing");
        user.setLastName("User");
        user.setActive(true);
        user.setRoles("ROLE_USER");
        user.setCreatedDate(LocalDateTime.now());
        userRepository.save(user);
    }

    @State("valid refresh token exists")
    public void validRefreshTokenExists() {
        System.out.println("Setting up: Valid refresh token exists");

        // Mock refresh token validation
        when(jwtTokenProvider.validateRefreshToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromRefreshToken(anyString())).thenReturn(1L);
    }

    @State("refresh token is invalid or expired")
    public void invalidRefreshToken() {
        System.out.println("Setting up: Refresh token is invalid or expired");

        // Mock refresh token validation failure
        when(jwtTokenProvider.validateRefreshToken(anyString())).thenReturn(false);
    }

    @State("user is authenticated")
    public void userIsAuthenticated() {
        System.out.println("Setting up: User is authenticated");

        // Clear and create authenticated user
        userRepository.deleteAll();

        User user = new User();
        user.setId(3L);
        user.setUsername("currentuser@example.com");
        user.setEmail("currentuser@example.com");
        user.setPassword(passwordEncoder.encode("CurrentPassword123!"));
        user.setFirstName("Current");
        user.setLastName("User");
        user.setActive(true);
        user.setRoles("ROLE_USER,ROLE_ADMIN");
        user.setLastLogin(LocalDateTime.now());
        user.setCreatedDate(LocalDateTime.now().minusDays(30));
        userRepository.save(user);

        // Mock JWT validation
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(3L);
        when(jwtTokenProvider.getRolesFromToken(anyString()))
                .thenReturn(List.of("ROLE_USER", "ROLE_ADMIN"));
    }

    @State("user is authenticated with valid current password")
    public void userAuthenticatedWithValidPassword() {
        System.out.println("Setting up: User authenticated with valid current password");

        // Clear and create user with known password
        userRepository.deleteAll();

        User user = new User();
        user.setId(4L);
        user.setUsername("passworduser@example.com");
        user.setEmail("passworduser@example.com");
        user.setPassword(passwordEncoder.encode("OldPassword123!"));
        user.setFirstName("Password");
        user.setLastName("User");
        user.setActive(true);
        user.setRoles("ROLE_USER");
        user.setCreatedDate(LocalDateTime.now());
        userRepository.save(user);

        // Mock JWT validation
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(4L);
    }

    @State("user is authenticated as admin")
    public void userAuthenticatedAsAdmin() {
        System.out.println("Setting up: User authenticated as admin");

        // Clear and create admin user
        userRepository.deleteAll();

        // Create admin user
        User admin = new User();
        admin.setId(5L);
        admin.setUsername("admin@example.com");
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("AdminPassword123!"));
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setActive(true);
        admin.setRoles("ROLE_USER,ROLE_ADMIN");
        admin.setCreatedDate(LocalDateTime.now());
        userRepository.save(admin);

        // Create some additional users for the list endpoint
        for (int i = 1; i <= 5; i++) {
            User user = new User();
            user.setId(10L + i);
            user.setUsername("user" + i + "@example.com");
            user.setEmail("user" + i + "@example.com");
            user.setPassword(passwordEncoder.encode("UserPassword123!"));
            user.setFirstName("User" + i);
            user.setLastName("Test");
            user.setActive(true);
            user.setRoles("ROLE_USER");
            user.setLastLogin(LocalDateTime.now().minusDays(i));
            user.setCreatedDate(LocalDateTime.now().minusDays(30 + i));
            userRepository.save(user);
        }

        // Mock JWT validation for admin
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(5L);
        when(jwtTokenProvider.getRolesFromToken(anyString()))
                .thenReturn(List.of("ROLE_USER", "ROLE_ADMIN"));
    }

    @State("user is authenticated as regular user")
    public void userAuthenticatedAsRegularUser() {
        System.out.println("Setting up: User authenticated as regular user (non-admin)");

        // Clear and create regular user
        userRepository.deleteAll();

        User user = new User();
        user.setId(6L);
        user.setUsername("regular@example.com");
        user.setEmail("regular@example.com");
        user.setPassword(passwordEncoder.encode("RegularPassword123!"));
        user.setFirstName("Regular");
        user.setLastName("User");
        user.setActive(true);
        user.setRoles("ROLE_USER");
        user.setCreatedDate(LocalDateTime.now());
        userRepository.save(user);

        // Mock JWT validation for regular user
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(6L);
        when(jwtTokenProvider.getRolesFromToken(anyString()))
                .thenReturn(List.of("ROLE_USER"));
    }
}

/**
 * WHY AUTH CONTRACT TESTS ARE CRITICAL:
 *
 * 1. Authentication is the gatekeeper to the entire platform
 * 2. Token format changes break ALL API calls
 * 3. Role-based access failures expose security vulnerabilities
 * 4. Password change bugs can lock users out
 * 5. Registration failures prevent onboarding
 *
 * These tests ensure:
 * - Frontend and backend agree on auth field names
 * - Token formats remain consistent
 * - Password validation works correctly
 * - Role-based access control is enforced
 * - Error responses match expectations
 *
 * Benefits:
 * - Catch auth bugs in seconds, not hours
 * - Prevent production auth failures
 * - Ensure security measures work correctly
 * - Validate user management flows
 */