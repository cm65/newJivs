package com.jivs.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivs.platform.domain.Extraction;
import com.jivs.platform.domain.ExtractionConfig;
import com.jivs.platform.domain.User;
import com.jivs.platform.repository.ExtractionRepository;
import com.jivs.platform.repository.ExtractionConfigRepository;
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
class ExtractionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExtractionRepository extractionRepository;

    @Autowired
    private ExtractionConfigRepository configRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User testUser;
    private String authToken;
    private Extraction testExtraction;
    private ExtractionConfig testConfig;

    @BeforeEach
    void setUp() {
        // Clean up
        extractionRepository.deleteAll();
        configRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user with DATA_ENGINEER role
        testUser = new User();
        testUser.setId(UUID.randomUUID().toString());
        testUser.setUsername("engineer");
        testUser.setEmail("engineer@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setFirstName("Data");
        testUser.setLastName("Engineer");
        testUser.setEnabled(true);
        testUser.setRoles(Collections.singleton("ROLE_DATA_ENGINEER"));
        testUser.setCreatedAt(new Date());
        testUser.setUpdatedAt(new Date());
        testUser = userRepository.save(testUser);

        // Generate auth token
        authToken = jwtTokenProvider.generateToken(testUser.getUsername());

        // Create test extraction
        testExtraction = new Extraction();
        testExtraction.setId(UUID.randomUUID().toString());
        testExtraction.setName("Test Extraction");
        testExtraction.setSourceType("JDBC");
        testExtraction.setStatus("PENDING");
        testExtraction.setRecordsExtracted(0L);
        testExtraction.setCreatedAt(new Date());
        testExtraction.setUpdatedAt(new Date());
        testExtraction = extractionRepository.save(testExtraction);

        // Create test config
        testConfig = new ExtractionConfig();
        testConfig.setId(UUID.randomUUID().toString());
        testConfig.setExtractionId(testExtraction.getId());
        testConfig.setConnectionString("jdbc:postgresql://localhost:5432/test");
        testConfig.setUsername("test_user");
        testConfig.setPassword("test_pass");
        testConfig.setQuery("SELECT * FROM test_table");
        testConfig.setCreatedAt(new Date());
        testConfig = configRepository.save(testConfig);
    }

    @Test
    void testCreateExtraction_Success() throws Exception {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("name", "New Extraction");
        request.put("sourceType", "JDBC");
        request.put("extractionQuery", "SELECT * FROM users");

        Map<String, String> connectionConfig = new HashMap<>();
        connectionConfig.put("connectionString", "jdbc:postgresql://localhost:5432/db");
        connectionConfig.put("username", "user");
        connectionConfig.put("password", "pass");
        request.put("connectionConfig", connectionConfig);

        // Act & Assert
        mockMvc.perform(post("/api/v1/extractions")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name", is("New Extraction")))
                .andExpect(jsonPath("$.data.sourceType", is("JDBC")))
                .andExpect(jsonPath("$.data.status", is("PENDING")));
    }

    @Test
    void testCreateExtraction_Unauthorized() throws Exception {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("name", "New Extraction");
        request.put("sourceType", "JDBC");

        // Act & Assert
        mockMvc.perform(post("/api/v1/extractions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateExtraction_InvalidSourceType() throws Exception {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("name", "New Extraction");
        request.put("sourceType", "INVALID_TYPE");

        // Act & Assert
        mockMvc.perform(post("/api/v1/extractions")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllExtractions_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/extractions")
                .header("Authorization", "Bearer " + authToken)
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", isA(List.class)))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].name", is("Test Extraction")));
    }

    @Test
    void testGetAllExtractions_WithStatusFilter() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/extractions")
                .header("Authorization", "Bearer " + authToken)
                .param("status", "PENDING")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", isA(List.class)))
                .andExpect(jsonPath("$.data.content[0].status", is("PENDING")));
    }

    @Test
    void testGetExtraction_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/extractions/{id}", testExtraction.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(testExtraction.getId())))
                .andExpect(jsonPath("$.data.name", is("Test Extraction")))
                .andExpect(jsonPath("$.data.sourceType", is("JDBC")));
    }

    @Test
    void testGetExtraction_NotFound() throws Exception {
        // Arrange
        String nonExistentId = UUID.randomUUID().toString();

        // Act & Assert
        mockMvc.perform(get("/api/v1/extractions/{id}", nonExistentId)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void testStartExtraction_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/extractions/{id}/start", testExtraction.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("started")));
    }

    @Test
    void testStartExtraction_AlreadyRunning() throws Exception {
        // Arrange - First start
        testExtraction.setStatus("RUNNING");
        extractionRepository.save(testExtraction);

        // Act & Assert
        mockMvc.perform(post("/api/v1/extractions/{id}/start", testExtraction.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testStopExtraction_Success() throws Exception {
        // Arrange
        testExtraction.setStatus("RUNNING");
        extractionRepository.save(testExtraction);

        // Act & Assert
        mockMvc.perform(post("/api/v1/extractions/{id}/stop", testExtraction.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("stopped")));
    }

    @Test
    void testStopExtraction_NotRunning() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/extractions/{id}/stop", testExtraction.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDeleteExtraction_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/extractions/{id}", testExtraction.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("deleted")));

        // Verify deletion
        mockMvc.perform(get("/api/v1/extractions/{id}", testExtraction.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteExtraction_Running_Forbidden() throws Exception {
        // Arrange
        testExtraction.setStatus("RUNNING");
        extractionRepository.save(testExtraction);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/extractions/{id}", testExtraction.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetStatistics_Success() throws Exception {
        // Arrange
        testExtraction.setRecordsExtracted(1000L);
        testExtraction.setStartTime(new Date(System.currentTimeMillis() - 60000));
        testExtraction.setEndTime(new Date());
        extractionRepository.save(testExtraction);

        // Act & Assert
        mockMvc.perform(get("/api/v1/extractions/{id}/statistics", testExtraction.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordsExtracted", is(1000)))
                .andExpect(jsonPath("$.data.startTime", notNullValue()))
                .andExpect(jsonPath("$.data.endTime", notNullValue()));
    }

    @Test
    void testTestConnection_Success() throws Exception {
        // Arrange
        Map<String, String> connectionConfig = new HashMap<>();
        connectionConfig.put("type", "JDBC");
        connectionConfig.put("connectionString", "jdbc:h2:mem:testdb");
        connectionConfig.put("username", "sa");
        connectionConfig.put("password", "");

        // Act & Assert
        mockMvc.perform(post("/api/v1/extractions/test-connection")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(connectionConfig)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success", notNullValue()));
    }

    @Test
    void testTestConnection_InvalidConfig() throws Exception {
        // Arrange
        Map<String, String> connectionConfig = new HashMap<>();
        connectionConfig.put("type", "INVALID");

        // Act & Assert
        mockMvc.perform(post("/api/v1/extractions/test-connection")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(connectionConfig)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetLogs_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/extractions/{id}/logs", testExtraction.getId())
                .header("Authorization", "Bearer " + authToken)
                .param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", isA(List.class)));
    }

    @Test
    void testPagination() throws Exception {
        // Arrange - Create multiple extractions
        for (int i = 0; i < 25; i++) {
            Extraction extraction = new Extraction();
            extraction.setId(UUID.randomUUID().toString());
            extraction.setName("Extraction " + i);
            extraction.setSourceType("JDBC");
            extraction.setStatus("PENDING");
            extraction.setRecordsExtracted(0L);
            extraction.setCreatedAt(new Date());
            extraction.setUpdatedAt(new Date());
            extractionRepository.save(extraction);
        }

        // Act & Assert - Page 0
        mockMvc.perform(get("/api/v1/extractions")
                .header("Authorization", "Bearer " + authToken)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(10)))
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(25)))
                .andExpect(jsonPath("$.data.totalPages", greaterThanOrEqualTo(3)));

        // Act & Assert - Page 1
        mockMvc.perform(get("/api/v1/extractions")
                .header("Authorization", "Bearer " + authToken)
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(10)));
    }

    @Test
    void testRoleBasedAccess_Admin() throws Exception {
        // Arrange - Create admin user
        User adminUser = new User();
        adminUser.setId(UUID.randomUUID().toString());
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode("password123"));
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setEnabled(true);
        adminUser.setRoles(Collections.singleton("ROLE_ADMIN"));
        adminUser.setCreatedAt(new Date());
        adminUser.setUpdatedAt(new Date());
        userRepository.save(adminUser);

        String adminToken = jwtTokenProvider.generateToken(adminUser.getUsername());

        // Act & Assert - Admin should have access
        mockMvc.perform(get("/api/v1/extractions")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testRoleBasedAccess_RegularUser_Forbidden() throws Exception {
        // Arrange - Create regular user without required role
        User regularUser = new User();
        regularUser.setId(UUID.randomUUID().toString());
        regularUser.setUsername("regular");
        regularUser.setEmail("regular@example.com");
        regularUser.setPassword(passwordEncoder.encode("password123"));
        regularUser.setFirstName("Regular");
        regularUser.setLastName("User");
        regularUser.setEnabled(true);
        regularUser.setRoles(Collections.singleton("ROLE_USER"));
        regularUser.setCreatedAt(new Date());
        regularUser.setUpdatedAt(new Date());
        userRepository.save(regularUser);

        String regularToken = jwtTokenProvider.generateToken(regularUser.getUsername());

        // Act & Assert - Regular user should be forbidden
        mockMvc.perform(get("/api/v1/extractions")
                .header("Authorization", "Bearer " + regularToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testConcurrentOperations() throws Exception {
        // Test that multiple users can interact with different extractions simultaneously

        // Create second extraction
        Extraction extraction2 = new Extraction();
        extraction2.setId(UUID.randomUUID().toString());
        extraction2.setName("Second Extraction");
        extraction2.setSourceType("SAP");
        extraction2.setStatus("PENDING");
        extraction2.setRecordsExtracted(0L);
        extraction2.setCreatedAt(new Date());
        extraction2.setUpdatedAt(new Date());
        extractionRepository.save(extraction2);

        // Both extractions should be accessible
        mockMvc.perform(get("/api/v1/extractions/{id}", testExtraction.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/extractions/{id}", extraction2.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());
    }

    @Test
    void testErrorHandling_DatabaseError() throws Exception {
        // Arrange - Try to get extraction with malformed ID
        String malformedId = "not-a-uuid";

        // Act & Assert
        mockMvc.perform(get("/api/v1/extractions/{id}", malformedId)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testExtractionLifecycle() throws Exception {
        // Create -> Start -> Stop -> Delete workflow

        // 1. Create
        Map<String, Object> request = new HashMap<>();
        request.put("name", "Lifecycle Test");
        request.put("sourceType", "JDBC");
        request.put("extractionQuery", "SELECT * FROM lifecycle");

        Map<String, String> connectionConfig = new HashMap<>();
        connectionConfig.put("connectionString", "jdbc:h2:mem:lifecycle");
        connectionConfig.put("username", "sa");
        connectionConfig.put("password", "");
        request.put("connectionConfig", connectionConfig);

        String createResponse = mockMvc.perform(post("/api/v1/extractions")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String extractionId = objectMapper.readTree(createResponse)
                .get("data").get("id").asText();

        // 2. Start
        mockMvc.perform(post("/api/v1/extractions/{id}/start", extractionId)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        // 3. Stop
        mockMvc.perform(post("/api/v1/extractions/{id}/stop", extractionId)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        // 4. Delete
        mockMvc.perform(delete("/api/v1/extractions/{id}", extractionId)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        // 5. Verify deletion
        mockMvc.perform(get("/api/v1/extractions/{id}", extractionId)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }
}
