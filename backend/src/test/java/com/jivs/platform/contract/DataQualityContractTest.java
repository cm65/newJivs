package com.jivs.platform.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.jivs.platform.domain.quality.DataQualityRule;
import com.jivs.platform.domain.quality.QualityDimension;
import com.jivs.platform.domain.quality.RuleType;
import com.jivs.platform.domain.quality.Severity;
import com.jivs.platform.domain.quality.DataQualityIssue;
import com.jivs.platform.domain.quality.IssueStatus;
import com.jivs.platform.repository.DataQualityRuleRepository;
import com.jivs.platform.repository.DataQualityIssueRepository;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Data Quality Contract Test - Provider Side
 *
 * This test verifies that the backend data quality endpoints
 * satisfy the contracts defined by the frontend.
 *
 * Data Quality is critical for:
 * - Ensuring data integrity and trust
 * - Rule-based validation
 * - Issue detection and tracking
 * - Quality score calculation
 * - Dataset profiling
 *
 * Coverage: 8/8 Data Quality endpoints
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("JiVS Backend")
@PactFolder("../frontend/pacts")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class DataQualityContractTest {

    @LocalServerPort
    private int port;

    @Autowired
    private DataQualityRuleRepository ruleRepository;

    @Autowired
    private DataQualityIssueRepository issueRepository;

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
     * Provider States - Set up test data for different data quality scenarios
     */

    @State("user is authenticated")
    public void userIsAuthenticated() {
        System.out.println("Setting up: User is authenticated");
        // Authentication is mocked in setup()
    }

    @State("user is authenticated and quality data exists")
    public void userAuthenticatedWithQualityData() {
        System.out.println("Setting up: User is authenticated and quality data exists");

        // Clear existing data
        ruleRepository.deleteAll();
        issueRepository.deleteAll();

        // Create test rules
        for (int i = 1; i <= 5; i++) {
            DataQualityRule rule = new DataQualityRule();
            rule.setId(UUID.randomUUID().toString());
            rule.setName("Quality Rule " + i);
            rule.setDescription("Description for rule " + i);
            rule.setDimension(QualityDimension.values()[i % QualityDimension.values().length]);
            rule.setRuleType(RuleType.NULL_CHECK);
            rule.setExpression("IS NOT NULL");
            rule.setTargetTable("customers");
            rule.setTargetColumn("column" + i);
            rule.setSeverity(Severity.HIGH);
            rule.setEnabled(true);
            rule.setExecutionCount(100L);
            rule.setLastExecuted(LocalDateTime.now().minusHours(i));
            rule.setPassRate(95.0 - i);
            rule.setCreatedDate(LocalDateTime.now().minusDays(i));
            ruleRepository.save(rule);
        }

        // Create test issues
        for (int i = 1; i <= 10; i++) {
            DataQualityIssue issue = new DataQualityIssue();
            issue.setId(UUID.randomUUID().toString());
            issue.setRuleId(UUID.randomUUID().toString());
            issue.setRuleName("Email Validation");
            issue.setDimension(QualityDimension.VALIDITY);
            issue.setSeverity(i % 2 == 0 ? Severity.HIGH : Severity.MEDIUM);
            issue.setStatus(IssueStatus.OPEN);
            issue.setTableName("customers");
            issue.setColumnName("email");
            issue.setRecordId("CUST-00" + i);
            issue.setFieldValue("invalid-email" + i);
            issue.setReason("Does not match email format");
            issue.setDetectedAt(LocalDateTime.now().minusDays(i));
            issueRepository.save(issue);
        }
    }

    @State("user is authenticated and rules exist")
    public void userAuthenticatedWithRules() {
        System.out.println("Setting up: User is authenticated and rules exist");

        // Clear existing data
        ruleRepository.deleteAll();

        // Create test rules with COMPLETENESS dimension
        for (int i = 1; i <= 3; i++) {
            DataQualityRule rule = new DataQualityRule();
            rule.setId(UUID.randomUUID().toString());
            rule.setName("Null Check Rule " + i);
            rule.setDescription("Check for null values in field " + i);
            rule.setDimension(QualityDimension.COMPLETENESS);
            rule.setRuleType(RuleType.NULL_CHECK);
            rule.setExpression("IS NOT NULL");
            rule.setTargetTable("customers");
            rule.setTargetColumn("field" + i);
            rule.setSeverity(Severity.HIGH);
            rule.setEnabled(true);
            rule.setLastExecuted(LocalDateTime.now().minusHours(i));
            rule.setPassRate(95.0 + i);
            rule.setCreatedDate(LocalDateTime.now().minusDays(i));
            ruleRepository.save(rule);
        }
    }

    @State("quality rule exists")
    public void qualityRuleExists() {
        System.out.println("Setting up: Quality rule exists");

        // Clear existing data
        ruleRepository.deleteAll();

        // Create specific rules for different operations
        DataQualityRule rule1 = new DataQualityRule();
        rule1.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440200").toString());
        rule1.setName("Completeness Check");
        rule1.setDescription("Check for null values in required fields");
        rule1.setDimension(QualityDimension.COMPLETENESS);
        rule1.setRuleType(RuleType.NULL_CHECK);
        rule1.setExpression("IS NOT NULL");
        rule1.setTargetTable("customers");
        rule1.setTargetColumn("email");
        rule1.setSeverity(Severity.HIGH);
        rule1.setEnabled(true);
        rule1.setExecutionCount(100L);
        rule1.setLastExecuted(LocalDateTime.now().minusHours(2));
        rule1.setPassRate(98.5);
        rule1.setCreatedDate(LocalDateTime.now().minusDays(7));
        ruleRepository.save(rule1);

        // Rule for update
        DataQualityRule rule2 = new DataQualityRule();
        rule2.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440201").toString());
        rule2.setName("Email Validation");
        rule2.setDescription("Validate email format");
        rule2.setDimension(QualityDimension.VALIDITY);
        rule2.setRuleType(RuleType.REGEX);
        rule2.setExpression("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
        rule2.setTargetTable("customers");
        rule2.setTargetColumn("email");
        rule2.setSeverity(Severity.HIGH);
        rule2.setEnabled(true);
        rule2.setCreatedDate(LocalDateTime.now().minusDays(5));
        ruleRepository.save(rule2);

        // Rule for deletion
        DataQualityRule rule3 = new DataQualityRule();
        rule3.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440202").toString());
        rule3.setName("Rule to Delete");
        rule3.setDimension(QualityDimension.ACCURACY);
        rule3.setRuleType(RuleType.RANGE_CHECK);
        rule3.setSeverity(Severity.LOW);
        rule3.setEnabled(false);
        rule3.setCreatedDate(LocalDateTime.now());
        ruleRepository.save(rule3);
    }

    @State("quality rule exists and can be executed")
    public void qualityRuleExistsAndCanBeExecuted() {
        System.out.println("Setting up: Quality rule exists and can be executed");

        // Clear existing data
        ruleRepository.deleteAll();

        // Create executable rule
        DataQualityRule rule = new DataQualityRule();
        rule.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440203").toString());
        rule.setName("Executable Rule");
        rule.setDescription("Rule ready for execution");
        rule.setDimension(QualityDimension.VALIDITY);
        rule.setRuleType(RuleType.REGEX);
        rule.setExpression("^[A-Z]{2}[0-9]{4}$");
        rule.setTargetTable("products");
        rule.setTargetColumn("product_code");
        rule.setSeverity(Severity.MEDIUM);
        rule.setEnabled(true);
        rule.setCreatedDate(LocalDateTime.now().minusDays(3));
        ruleRepository.save(rule);
    }

    @State("quality issues exist")
    public void qualityIssuesExist() {
        System.out.println("Setting up: Quality issues exist");

        // Clear existing data
        issueRepository.deleteAll();

        // Create test issues with HIGH severity and OPEN status
        for (int i = 1; i <= 5; i++) {
            DataQualityIssue issue = new DataQualityIssue();
            issue.setId(UUID.randomUUID().toString());
            issue.setRuleId(UUID.randomUUID().toString());
            issue.setRuleName("Email Validation");
            issue.setDimension(QualityDimension.VALIDITY);
            issue.setSeverity(Severity.HIGH);
            issue.setStatus(IssueStatus.OPEN);
            issue.setTableName("customers");
            issue.setColumnName("email");
            issue.setRecordId("CUST-00" + i);
            issue.setFieldValue("invalid-email" + i);
            issue.setReason("Does not match email format");
            issue.setDetectedAt(LocalDateTime.now().minusHours(i));
            issueRepository.save(issue);
        }
    }

    @State("user is authenticated and table exists")
    public void userAuthenticatedAndTableExists() {
        System.out.println("Setting up: User is authenticated and table exists");
        // In a real implementation, this would verify the table exists in the database
        // For contract testing, the controller would handle the profiling logic
    }
}

/**
 * WHY DATA QUALITY CONTRACT TESTS ARE CRITICAL:
 *
 * 1. Data quality directly impacts business decisions
 * 2. Quality dimensions must be consistently defined
 * 3. Rule expressions need exact syntax validation
 * 4. Severity levels drive alerting and prioritization
 * 5. Issue tracking enables remediation workflows
 *
 * These tests ensure:
 * - Frontend and backend agree on quality dimensions
 * - Rule types and expressions are validated
 * - Score calculations are consistent
 * - Issue severity matches across systems
 * - Profiling results have expected structure
 *
 * Benefits:
 * - Catch rule syntax errors before execution
 * - Prevent score calculation mismatches
 * - Ensure issue tracking consistency
 * - Validate profiling recommendations
 * - Maintain quality metric accuracy
 */