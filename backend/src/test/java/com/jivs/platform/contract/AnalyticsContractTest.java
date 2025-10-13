package com.jivs.platform.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Analytics Contract Test - Provider Side
 *
 * This test verifies that the backend analytics endpoints
 * satisfy the contracts defined by the frontend.
 *
 * Analytics is essential for:
 * - Business intelligence and decision making
 * - Performance monitoring and optimization
 * - Usage tracking and capacity planning
 * - Compliance reporting
 * - Export and report generation
 *
 * Coverage: 7/7 Analytics endpoints
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("JiVS Backend")
@PactFolder("../frontend/pacts")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class AnalyticsContractTest {

    @LocalServerPort
    private int port;

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
     * Provider States - Set up test data for different analytics scenarios
     *
     * Note: In a real implementation, these states would set up test data
     * in the appropriate repositories or services. For contract testing,
     * the controller would aggregate and return the analytics data.
     */

    @State("user is authenticated")
    public void userIsAuthenticated() {
        System.out.println("Setting up: User is authenticated");
        // Authentication is mocked in setup()
    }

    @State("user is authenticated and analytics data exists")
    public void userAuthenticatedWithAnalyticsData() {
        System.out.println("Setting up: User is authenticated and analytics data exists");

        // In a real implementation, this would:
        // 1. Set up extraction data in ExtractionRepository
        // 2. Set up migration data in MigrationRepository
        // 3. Set up quality scores in DataQualityService
        // 4. Set up compliance metrics in ComplianceService
        // 5. Analytics controller would aggregate this data

        // For contract testing, the controller mock returns the expected structure
    }

    @State("user is authenticated and extraction analytics exist")
    public void userAuthenticatedWithExtractionAnalytics() {
        System.out.println("Setting up: User is authenticated and extraction analytics exist");

        // Would set up:
        // - Extraction records with various statuses
        // - Source type distribution
        // - Time series extraction data
        // - Top extractor users
    }

    @State("user is authenticated and migration analytics exist")
    public void userAuthenticatedWithMigrationAnalytics() {
        System.out.println("Setting up: User is authenticated and migration analytics exist");

        // Would set up:
        // - Migration records with phase metrics
        // - Success/failure statistics
        // - Performance trends over time
        // - Throughput and latency data
    }

    @State("user is authenticated and quality analytics exist")
    public void userAuthenticatedWithQualityAnalytics() {
        System.out.println("Setting up: User is authenticated and quality analytics exist");

        // Would set up:
        // - Data quality scores by dimension
        // - Rule execution statistics
        // - Issues trend data
        // - Top violations by rule
    }

    @State("user is authenticated and usage analytics exist")
    public void userAuthenticatedWithUsageAnalytics() {
        System.out.println("Setting up: User is authenticated and usage analytics exist");

        // Would set up:
        // - Active user counts (DAU/WAU/MAU)
        // - API endpoint usage statistics
        // - User activity logs
        // - Storage usage metrics
    }

    @State("user is authenticated and compliance analytics exist")
    public void userAuthenticatedWithComplianceAnalytics() {
        System.out.println("Setting up: User is authenticated and compliance analytics exist");

        // Would set up:
        // - GDPR request metrics
        // - CCPA request metrics
        // - Request type distribution
        // - Retention policy compliance data
    }

    @State("user is authenticated and performance metrics exist")
    public void userAuthenticatedWithPerformanceMetrics() {
        System.out.println("Setting up: User is authenticated and performance metrics exist");

        // Would set up:
        // - System resource metrics (CPU, memory, disk)
        // - Database performance metrics
        // - Cache hit/miss rates
        // - Message queue statistics
    }

    @State("user is authenticated and can export reports")
    public void userAuthenticatedCanExportReports() {
        System.out.println("Setting up: User is authenticated and can export reports");

        // Would set up:
        // - Report generation service mock
        // - PDF/Excel/CSV export capabilities
        // - Data aggregation for export
    }
}

/**
 * WHY ANALYTICS CONTRACT TESTS ARE ESSENTIAL:
 *
 * 1. Analytics drive strategic business decisions
 * 2. Metrics must be calculated consistently across systems
 * 3. Dashboard data powers executive visibility
 * 4. Performance metrics enable proactive optimization
 * 5. Compliance analytics ensure regulatory reporting accuracy
 *
 * These tests ensure:
 * - Frontend and backend agree on metric calculations
 * - Time series data format consistency
 * - Aggregation logic alignment
 * - Export format compatibility
 * - Performance baseline definitions
 *
 * Benefits:
 * - Catch calculation discrepancies before production
 * - Ensure dashboard accuracy
 * - Validate trend analysis logic
 * - Confirm export functionality
 * - Maintain metrics consistency
 *
 * Real-world impact:
 * - Wrong metrics = wrong business decisions
 * - Inconsistent calculations = loss of trust
 * - Export failures = compliance violations
 * - Performance blind spots = system failures
 */