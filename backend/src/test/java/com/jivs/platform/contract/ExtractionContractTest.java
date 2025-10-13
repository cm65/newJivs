package com.jivs.platform.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.jivs.platform.domain.extraction.Extraction;
import com.jivs.platform.domain.extraction.ExtractionStatus;
import com.jivs.platform.domain.extraction.SourceType;
import com.jivs.platform.repository.ExtractionRepository;
import com.jivs.platform.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Extraction Contract Test - Provider Side
 *
 * This test verifies that the backend extraction endpoints
 * satisfy the contracts defined by the frontend.
 *
 * Extraction is critical for:
 * - Data ingestion from multiple sources
 * - Connection management
 * - Query execution and validation
 * - Progress tracking
 * - Error handling
 *
 * Coverage: 9/9 Extraction endpoints
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("JiVS Backend")
@PactFolder("../frontend/pacts")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class ExtractionContractTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ExtractionRepository extractionRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setup(PactVerificationContext context) {
        // Configure the test to hit our running Spring Boot application
        context.setTarget(new HttpTestTarget("localhost", port));

        // Mock JWT validation for tests
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(1L);
        when(jwtTokenProvider.getRolesFromToken(anyString())).thenReturn(Collections.singletonList("ROLE_ADMIN"));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        // This will run once for each interaction in the pact file
        context.verifyInteraction();
    }

    /**
     * Provider States - Set up test data for different extraction scenarios
     */

    @State("user is authenticated")
    public void userIsAuthenticated() {
        System.out.println("Setting up: User is authenticated");
        // Authentication is mocked in setup()
    }

    @State("user is authenticated and extractions exist")
    public void userAuthenticatedWithExtractions() {
        System.out.println("Setting up: User is authenticated and extractions exist");

        // Clear existing data
        extractionRepository.deleteAll();

        // Create test extractions
        Extraction extraction1 = new Extraction();
        extraction1.setId(UUID.randomUUID().toString());
        extraction1.setName("Data Extraction Job 1");
        extraction1.setDescription("Test extraction 1");
        extraction1.setSourceType(SourceType.JDBC);
        extraction1.setStatus(ExtractionStatus.RUNNING);
        extraction1.setRecordsExtracted(5000L);
        extraction1.setTotalRecords(10000L);
        extraction1.setExtractionQuery("SELECT * FROM customers");
        extraction1.setSchedule("0 0 * * * *");
        extraction1.setCreatedDate(LocalDateTime.now().minusDays(1));
        extractionRepository.save(extraction1);

        Extraction extraction2 = new Extraction();
        extraction2.setId(UUID.randomUUID().toString());
        extraction2.setName("Data Extraction Job 2");
        extraction2.setDescription("Test extraction 2");
        extraction2.setSourceType(SourceType.SAP);
        extraction2.setStatus(ExtractionStatus.RUNNING);
        extraction2.setRecordsExtracted(3000L);
        extraction2.setTotalRecords(8000L);
        extraction2.setCreatedDate(LocalDateTime.now().minusDays(2));
        extractionRepository.save(extraction2);
    }

    @State("extraction exists")
    public void extractionExists() {
        System.out.println("Setting up: Extraction exists");

        // Clear existing data
        extractionRepository.deleteAll();

        // Create a specific extraction
        Extraction extraction = new Extraction();
        extraction.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440100").toString());
        extraction.setName("Production Data Extract");
        extraction.setDescription("Daily production data extraction");
        extraction.setSourceType(SourceType.JDBC);
        extraction.setStatus(ExtractionStatus.RUNNING);
        extraction.setRecordsExtracted(5000L);
        extraction.setTotalRecords(10000L);
        extraction.setExtractionQuery("SELECT * FROM transactions");
        extraction.setSchedule("0 0 * * * *");
        extraction.setStartedAt(LocalDateTime.now().minusHours(1));
        extraction.setCreatedDate(LocalDateTime.now().minusDays(5));
        extractionRepository.save(extraction);

        // Also create extraction for deletion
        Extraction extractionToDelete = new Extraction();
        extractionToDelete.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440103").toString());
        extractionToDelete.setName("Extraction to Delete");
        extractionToDelete.setSourceType(SourceType.JDBC);
        extractionToDelete.setStatus(ExtractionStatus.PENDING);
        extractionToDelete.setCreatedDate(LocalDateTime.now());
        extractionRepository.save(extractionToDelete);
    }

    @State("extraction exists in PENDING status")
    public void extractionInPendingStatus() {
        System.out.println("Setting up: Extraction exists in PENDING status");

        // Clear existing data
        extractionRepository.deleteAll();

        // Create pending extraction
        Extraction extraction = new Extraction();
        extraction.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440101").toString());
        extraction.setName("Pending Extraction");
        extraction.setDescription("Ready to start");
        extraction.setSourceType(SourceType.JDBC);
        extraction.setStatus(ExtractionStatus.PENDING);
        extraction.setRecordsExtracted(0L);
        extraction.setTotalRecords(0L);
        extraction.setCreatedDate(LocalDateTime.now());
        extractionRepository.save(extraction);
    }

    @State("extraction exists in RUNNING status")
    public void extractionInRunningStatus() {
        System.out.println("Setting up: Extraction exists in RUNNING status");

        // Clear existing data
        extractionRepository.deleteAll();

        // Create running extraction
        Extraction extraction = new Extraction();
        extraction.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440102").toString());
        extraction.setName("Running Extraction");
        extraction.setDescription("Currently extracting data");
        extraction.setSourceType(SourceType.JDBC);
        extraction.setStatus(ExtractionStatus.RUNNING);
        extraction.setRecordsExtracted(2500L);
        extraction.setTotalRecords(10000L);
        extraction.setStartedAt(LocalDateTime.now().minusMinutes(30));
        extraction.setCreatedDate(LocalDateTime.now());
        extractionRepository.save(extraction);
    }

    @State("extraction exists with statistics")
    public void extractionExistsWithStatistics() {
        System.out.println("Setting up: Extraction exists with statistics");

        // Clear existing data
        extractionRepository.deleteAll();

        // Create extraction with statistics
        Extraction extraction = new Extraction();
        extraction.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440104").toString());
        extraction.setName("Statistical Extraction");
        extraction.setDescription("Extraction with statistics");
        extraction.setSourceType(SourceType.JDBC);
        extraction.setStatus(ExtractionStatus.COMPLETED);
        extraction.setRecordsExtracted(8500L);
        extraction.setTotalRecords(10000L);
        extraction.setRecordsFailed(100L);
        extraction.setRecordsSkipped(50L);
        extraction.setDataSize(1048576L); // 1MB
        extraction.setStartedAt(LocalDateTime.now().minusHours(2));
        extraction.setCompletedAt(LocalDateTime.now().minusHours(1));
        extraction.setCreatedDate(LocalDateTime.now().minusDays(1));
        extractionRepository.save(extraction);
    }

    @State("extraction exists with logs")
    public void extractionExistsWithLogs() {
        System.out.println("Setting up: Extraction exists with logs");

        // Clear existing data
        extractionRepository.deleteAll();

        // Create extraction with logs
        Extraction extraction = new Extraction();
        extraction.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440105").toString());
        extraction.setName("Logged Extraction");
        extraction.setDescription("Extraction with log entries");
        extraction.setSourceType(SourceType.JDBC);
        extraction.setStatus(ExtractionStatus.RUNNING);
        extraction.setRecordsExtracted(5000L);
        extraction.setTotalRecords(10000L);
        extraction.setStartedAt(LocalDateTime.now().minusHours(1));
        extraction.setCreatedDate(LocalDateTime.now());

        // In a real implementation, logs would be stored separately
        // For contract testing, the controller would fetch and return logs
        extractionRepository.save(extraction);
    }
}

/**
 * WHY EXTRACTION CONTRACT TESTS ARE CRITICAL:
 *
 * 1. Extraction is the entry point for all data into JiVS
 * 2. Connection configuration must be exact for each source type
 * 3. Query validation prevents SQL injection and errors
 * 4. Status transitions must be consistent
 * 5. Statistics are crucial for monitoring and optimization
 *
 * These tests ensure:
 * - Frontend and backend agree on connection config structure
 * - Source types are validated consistently
 * - Status transitions follow the correct state machine
 * - Statistics calculations match expectations
 * - Error messages are informative and consistent
 *
 * Benefits:
 * - Catch connection config bugs before deployment
 * - Prevent data extraction failures
 * - Ensure progress tracking accuracy
 * - Validate error handling paths
 */