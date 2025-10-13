package com.jivs.platform.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for MigrationController
 * THIS TEST WOULD HAVE CAUGHT THE MIGRATION CREATION ERROR
 */
@SpringBootTest
@AutoConfigureMockMvc
class MigrationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateMigration_Success() throws Exception {
        String requestBody = """
            {
              "name": "Test Migration",
              "description": "Integration test migration",
              "sourceSystem": "Oracle",
              "targetSystem": "PostgreSQL",
              "migrationType": "FULL"
            }
            """;

        mockMvc.perform(post("/api/v1/migrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Test Migration"))
                .andExpect(jsonPath("$.status").value("INITIALIZED"));

        // ↑ This test would have FAILED with:
        // "ERROR: null value in column project_code violates not-null constraint"
        // CATCHING THE BUG IMMEDIATELY
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateAndDeleteMigration_FullCycle() throws Exception {
        // Create
        String createResponse = mockMvc.perform(post("/api/v1/migrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\",\"sourceSystem\":\"A\",\"targetSystem\":\"B\",\"migrationType\":\"FULL\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String migrationId = extractId(createResponse);

        // Retrieve
        mockMvc.perform(get("/api/v1/migrations/" + migrationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(migrationId));

        // Delete
        mockMvc.perform(delete("/api/v1/migrations/" + migrationId))
                .andExpect(status().isOk());
    }

    private String extractId(String json) {
        // Simple extraction for test purposes
        return json.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }
}
