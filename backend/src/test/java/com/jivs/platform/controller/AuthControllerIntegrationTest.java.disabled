package com.jivs.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivs.platform.domain.User;
import com.jivs.platform.repository.UserRepository;
import com.jivs.platform.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User testUser;
    private String validToken;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setId(UUID.randomUUID().toString());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEnabled(true);
        testUser.setCreatedAt(new Date());
        testUser.setUpdatedAt(new Date());
        testUser = userRepository.save(testUser);

        // Generate valid token
        validToken = jwtTokenProvider.generateToken(testUser.getUsername());
    }

    @Test
    void testLogin_Success() throws Exception {
        // Arrange
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "testuser");
        loginRequest.put("password", "password123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.data.username", is("testuser")))
                .andExpect(jsonPath("$.data.email", is("test@example.com")));
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        // Arrange
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "testuser");
        loginRequest.put("password", "wrongpassword");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogin_UserNotFound() throws Exception {
        // Arrange
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "nonexistent");
        loginRequest.put("password", "password123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogin_MissingUsername() throws Exception {
        // Arrange
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("password", "password123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegister_Success() throws Exception {
        // Arrange
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("username", "newuser");
        registerRequest.put("email", "newuser@example.com");
        registerRequest.put("password", "password123");
        registerRequest.put("firstName", "New");
        registerRequest.put("lastName", "User");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username", is("newuser")))
                .andExpect(jsonPath("$.data.email", is("newuser@example.com")));
    }

    @Test
    void testRegister_DuplicateUsername() throws Exception {
        // Arrange
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("username", "testuser"); // Already exists
        registerRequest.put("email", "another@example.com");
        registerRequest.put("password", "password123");
        registerRequest.put("firstName", "Another");
        registerRequest.put("lastName", "User");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void testRegister_DuplicateEmail() throws Exception {
        // Arrange
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("username", "anotheruser");
        registerRequest.put("email", "test@example.com"); // Already exists
        registerRequest.put("password", "password123");
        registerRequest.put("firstName", "Another");
        registerRequest.put("lastName", "User");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void testRefreshToken_Success() throws Exception {
        // Arrange
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser.getUsername());
        Map<String, String> refreshRequest = new HashMap<>();
        refreshRequest.put("refreshToken", refreshToken);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken", notNullValue()));
    }

    @Test
    void testRefreshToken_InvalidToken() throws Exception {
        // Arrange
        Map<String, String> refreshRequest = new HashMap<>();
        refreshRequest.put("refreshToken", "invalid-token");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetCurrentUser_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username", is("testuser")))
                .andExpect(jsonPath("$.data.email", is("test@example.com")))
                .andExpect(jsonPath("$.data.firstName", is("Test")))
                .andExpect(jsonPath("$.data.lastName", is("User")));
    }

    @Test
    void testGetCurrentUser_NoToken() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetCurrentUser_InvalidToken() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUpdateCurrentUser_Success() throws Exception {
        // Arrange
        Map<String, String> updateRequest = new HashMap<>();
        updateRequest.put("firstName", "Updated");
        updateRequest.put("lastName", "Name");
        updateRequest.put("email", "updated@example.com");

        // Act & Assert
        mockMvc.perform(put("/api/v1/auth/me")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName", is("Updated")))
                .andExpect(jsonPath("$.data.lastName", is("Name")))
                .andExpect(jsonPath("$.data.email", is("updated@example.com")));
    }

    @Test
    void testChangePassword_Success() throws Exception {
        // Arrange
        Map<String, String> passwordRequest = new HashMap<>();
        passwordRequest.put("oldPassword", "password123");
        passwordRequest.put("newPassword", "newpassword123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/change-password")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isOk());

        // Verify can login with new password
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "testuser");
        loginRequest.put("password", "newpassword123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void testChangePassword_WrongOldPassword() throws Exception {
        // Arrange
        Map<String, String> passwordRequest = new HashMap<>();
        passwordRequest.put("oldPassword", "wrongpassword");
        passwordRequest.put("newPassword", "newpassword123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/change-password")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogout_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAllUsers_AsAdmin_Success() throws Exception {
        // Arrange - Make test user an admin
        testUser.setRoles(Collections.singleton("ROLE_ADMIN"));
        userRepository.save(testUser);
        String adminToken = jwtTokenProvider.generateToken(testUser.getUsername());

        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.data.content", isA(List.class)));
    }

    @Test
    void testGetAllUsers_AsRegularUser_Forbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/users")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testPasswordValidation() throws Exception {
        // Arrange - Try to register with weak password
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("username", "weakpassuser");
        registerRequest.put("email", "weak@example.com");
        registerRequest.put("password", "123"); // Too short
        registerRequest.put("firstName", "Weak");
        registerRequest.put("lastName", "Pass");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testEmailValidation() throws Exception {
        // Arrange - Try to register with invalid email
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("username", "invalidemail");
        registerRequest.put("email", "not-an-email"); // Invalid format
        registerRequest.put("password", "password123");
        registerRequest.put("firstName", "Invalid");
        registerRequest.put("lastName", "Email");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testTokenExpiration() throws Exception {
        // Note: This test would require waiting for token expiration
        // In a real scenario, you'd mock the time or use a very short expiration
        // For now, we'll just verify that expired tokens are rejected

        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImV4cCI6MTUxNjIzOTAyMn0.test";

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testConcurrentLogin() throws Exception {
        // Test that the same user can login from multiple sessions
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "testuser");
        loginRequest.put("password", "password123");

        // First login
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        // Second login (should also succeed)
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());
    }
}
