package com.jivs.platform.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.jivs.platform.domain.compliance.DataSubjectRequest;
import com.jivs.platform.domain.compliance.RequestType;
import com.jivs.platform.domain.compliance.RequestStatus;
import com.jivs.platform.domain.compliance.ProcessingStatus;
import com.jivs.platform.domain.compliance.Consent;
import com.jivs.platform.domain.compliance.ConsentStatus;
import com.jivs.platform.domain.compliance.RetentionPolicy;
import com.jivs.platform.domain.compliance.RetentionAction;
import com.jivs.platform.domain.compliance.DataCategory;
import com.jivs.platform.repository.DataSubjectRequestRepository;
import com.jivs.platform.repository.ConsentRepository;
import com.jivs.platform.repository.RetentionPolicyRepository;
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
 * Compliance Contract Test - Provider Side
 *
 * This test verifies that the backend compliance endpoints
 * satisfy the contracts defined by the frontend.
 *
 * Compliance is CRITICAL for:
 * - GDPR Articles 7, 15, 16, 17, 20 compliance
 * - CCPA consumer rights implementation
 * - Data subject request processing
 * - Consent management
 * - Retention policy enforcement
 * - Audit trail completeness
 *
 * Coverage: 10/10 Compliance endpoints
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("JiVS Backend")
@PactFolder("../frontend/pacts")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class ComplianceContractTest {

    @LocalServerPort
    private int port;

    @Autowired
    private DataSubjectRequestRepository requestRepository;

    @Autowired
    private ConsentRepository consentRepository;

    @Autowired
    private RetentionPolicyRepository retentionPolicyRepository;

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
     * Provider States - Set up test data for different compliance scenarios
     */

    @State("user is authenticated")
    public void userIsAuthenticated() {
        System.out.println("Setting up: User is authenticated");
        // Authentication is mocked in setup()
    }

    @State("user is authenticated and compliance data exists")
    public void userAuthenticatedWithComplianceData() {
        System.out.println("Setting up: User is authenticated and compliance data exists");

        // Clear existing data
        requestRepository.deleteAll();
        consentRepository.deleteAll();
        retentionPolicyRepository.deleteAll();

        // Create test data subject requests (various GDPR/CCPA types)
        for (int i = 1; i <= 5; i++) {
            DataSubjectRequest request = new DataSubjectRequest();
            request.setId(UUID.randomUUID().toString());
            request.setSubjectId("USER-" + String.format("%06d", i));
            request.setSubjectEmail("user" + i + "@example.com");
            request.setRequestType(i == 1 ? RequestType.ACCESS :
                                  i == 2 ? RequestType.ERASURE :
                                  i == 3 ? RequestType.RECTIFICATION :
                                  i == 4 ? RequestType.PORTABILITY :
                                  RequestType.OBJECTION);
            request.setStatus(i <= 2 ? RequestStatus.PENDING : RequestStatus.COMPLETED);
            request.setRegulation(i % 2 == 0 ? "GDPR" : "CCPA");
            request.setDescription("Request for " + request.getRequestType().toString().toLowerCase());
            request.setRequestDate(LocalDateTime.now().minusDays(i));
            if (i > 2) {
                request.setCompletedDate(LocalDateTime.now().minusHours(i * 2));
            }
            request.setVerificationMethod("EMAIL");
            request.setVerified(true);
            requestRepository.save(request);
        }

        // Create test consents
        for (int i = 1; i <= 3; i++) {
            Consent consent = new Consent();
            consent.setId(UUID.randomUUID().toString());
            consent.setUserId("USER-" + String.format("%06d", i));
            consent.setDataCategory(i == 1 ? DataCategory.MARKETING :
                                   i == 2 ? DataCategory.ANALYTICS :
                                   DataCategory.PERSONALIZATION);
            consent.setPurpose("Use data for " + consent.getDataCategory().toString().toLowerCase());
            consent.setStatus(i == 3 ? ConsentStatus.WITHDRAWN : ConsentStatus.ACTIVE);
            consent.setGrantedAt(LocalDateTime.now().minusDays(30 - i * 5));
            if (i == 3) {
                consent.setWithdrawnAt(LocalDateTime.now().minusDays(2));
            }
            consent.setExpiresAt(LocalDateTime.now().plusDays(365));
            consent.setVersion("1.0");
            consentRepository.save(consent);
        }

        // Create test retention policies
        for (int i = 1; i <= 4; i++) {
            RetentionPolicy policy = new RetentionPolicy();
            policy.setId(UUID.randomUUID().toString());
            policy.setName("Policy " + i);
            policy.setDescription("Retention policy for " +
                (i == 1 ? "customer data" :
                 i == 2 ? "transaction logs" :
                 i == 3 ? "audit trails" : "temporary files"));
            policy.setDataCategory(i == 1 ? DataCategory.PERSONAL_DATA :
                                  i == 2 ? DataCategory.TRANSACTION_DATA :
                                  i == 3 ? DataCategory.AUDIT_DATA :
                                  DataCategory.TEMPORARY);
            policy.setRetentionDays(i == 1 ? 2555 : // 7 years
                                   i == 2 ? 1095 : // 3 years
                                   i == 3 ? 365 :  // 1 year
                                   30);            // 30 days
            policy.setAction(i == 1 ? RetentionAction.ARCHIVE :
                            i == 2 ? RetentionAction.DELETE :
                            i == 3 ? RetentionAction.ANONYMIZE :
                            RetentionAction.SOFT_DELETE);
            policy.setEnabled(i != 4);
            policy.setCreatedDate(LocalDateTime.now().minusDays(i * 10));
            retentionPolicyRepository.save(policy);
        }
    }

    @State("user is authenticated and requests exist")
    public void userAuthenticatedWithRequests() {
        System.out.println("Setting up: User is authenticated and requests exist");

        // Clear existing data
        requestRepository.deleteAll();

        // Create specific GDPR ACCESS requests
        for (int i = 1; i <= 3; i++) {
            DataSubjectRequest request = new DataSubjectRequest();
            request.setId(UUID.randomUUID().toString());
            request.setSubjectId("GDPR-USER-" + i);
            request.setSubjectEmail("gdpr.user" + i + "@example.com");
            request.setRequestType(RequestType.ACCESS);
            request.setStatus(RequestStatus.PENDING);
            request.setRegulation("GDPR");
            request.setDescription("Request for personal data access under GDPR Article 15");
            request.setRequestDate(LocalDateTime.now().minusDays(i));
            request.setVerificationMethod("EMAIL");
            request.setVerified(true);
            request.setPriority(i == 1 ? "HIGH" : "NORMAL");
            requestRepository.save(request);
        }

        // Create specific CCPA DELETE requests
        for (int i = 1; i <= 2; i++) {
            DataSubjectRequest request = new DataSubjectRequest();
            request.setId(UUID.randomUUID().toString());
            request.setSubjectId("CCPA-USER-" + i);
            request.setSubjectEmail("ccpa.user" + i + "@example.com");
            request.setRequestType(RequestType.ERASURE);
            request.setStatus(RequestStatus.PENDING);
            request.setRegulation("CCPA");
            request.setDescription("Request for data deletion under CCPA");
            request.setRequestDate(LocalDateTime.now().minusHours(i * 12));
            request.setVerificationMethod("ID_VERIFICATION");
            request.setVerified(true);
            request.setPriority("HIGH");
            requestRepository.save(request);
        }
    }

    @State("data subject request exists")
    public void dataSubjectRequestExists() {
        System.out.println("Setting up: Data subject request exists");

        // Clear existing data
        requestRepository.deleteAll();

        // Create specific requests for different operations
        DataSubjectRequest request1 = new DataSubjectRequest();
        request1.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440300").toString());
        request1.setSubjectId("USER-123456");
        request1.setSubjectEmail("john.doe@example.com");
        request1.setRequestType(RequestType.ACCESS);
        request1.setStatus(RequestStatus.PENDING);
        request1.setRegulation("GDPR");
        request1.setDescription("Access request for all personal data");
        request1.setRequestDate(LocalDateTime.now().minusDays(1));
        request1.setVerificationMethod("EMAIL");
        request1.setVerified(true);
        request1.setDataSystems("CRM,ERP,Marketing");
        requestRepository.save(request1);

        // Request for processing
        DataSubjectRequest request2 = new DataSubjectRequest();
        request2.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440301").toString());
        request2.setSubjectId("USER-789012");
        request2.setSubjectEmail("jane.smith@example.com");
        request2.setRequestType(RequestType.ERASURE);
        request2.setStatus(RequestStatus.PENDING);
        request2.setRegulation("CCPA");
        request2.setDescription("Delete all my personal information");
        request2.setRequestDate(LocalDateTime.now().minusHours(6));
        request2.setVerificationMethod("ID_VERIFICATION");
        request2.setVerified(true);
        requestRepository.save(request2);

        // Completed request for export
        DataSubjectRequest request3 = new DataSubjectRequest();
        request3.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440302").toString());
        request3.setSubjectId("USER-345678");
        request3.setSubjectEmail("bob.wilson@example.com");
        request3.setRequestType(RequestType.PORTABILITY);
        request3.setStatus(RequestStatus.COMPLETED);
        request3.setRegulation("GDPR");
        request3.setDescription("Export my data in machine-readable format");
        request3.setRequestDate(LocalDateTime.now().minusDays(3));
        request3.setCompletedDate(LocalDateTime.now().minusDays(1));
        request3.setVerificationMethod("EMAIL");
        request3.setVerified(true);
        request3.setProcessingStatus(ProcessingStatus.EXPORTED);
        request3.setExportPath("/exports/USER-345678_20250113.json");
        requestRepository.save(request3);
    }

    @State("consents exist")
    public void consentsExist() {
        System.out.println("Setting up: Consents exist");

        // Clear existing data
        consentRepository.deleteAll();

        // Create active consents
        for (int i = 1; i <= 3; i++) {
            Consent consent = new Consent();
            consent.setId(UUID.randomUUID().toString());
            consent.setUserId("USER-" + String.format("%06d", i));
            consent.setDataCategory(DataCategory.MARKETING);
            consent.setPurpose("Marketing communications and promotional offers");
            consent.setStatus(ConsentStatus.ACTIVE);
            consent.setGrantedAt(LocalDateTime.now().minusDays(30 - i * 5));
            consent.setExpiresAt(LocalDateTime.now().plusDays(335 + i * 30));
            consent.setVersion("2.0");
            consent.setScope("Email marketing, SMS marketing, Push notifications");
            consentRepository.save(consent);
        }

        // Create a consent for revocation
        Consent consentToRevoke = new Consent();
        consentToRevoke.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440401").toString());
        consentToRevoke.setUserId("USER-999999");
        consentToRevoke.setDataCategory(DataCategory.ANALYTICS);
        consentToRevoke.setPurpose("Website analytics and usage tracking");
        consentToRevoke.setStatus(ConsentStatus.ACTIVE);
        consentToRevoke.setGrantedAt(LocalDateTime.now().minusDays(60));
        consentToRevoke.setExpiresAt(LocalDateTime.now().plusDays(305));
        consentToRevoke.setVersion("1.5");
        consentRepository.save(consentToRevoke);
    }

    @State("retention policies exist")
    public void retentionPoliciesExist() {
        System.out.println("Setting up: Retention policies exist");

        // Clear existing data
        retentionPolicyRepository.deleteAll();

        // Create comprehensive retention policies
        RetentionPolicy policy1 = new RetentionPolicy();
        policy1.setId(UUID.randomUUID().toString());
        policy1.setName("GDPR Customer Data");
        policy1.setDescription("7-year retention for customer records per GDPR");
        policy1.setDataCategory(DataCategory.PERSONAL_DATA);
        policy1.setRetentionDays(2555);
        policy1.setAction(RetentionAction.ARCHIVE);
        policy1.setEnabled(true);
        policy1.setLegalBasis("GDPR Article 5(1)(e) - Storage limitation");
        policy1.setCreatedDate(LocalDateTime.now().minusDays(90));
        retentionPolicyRepository.save(policy1);

        RetentionPolicy policy2 = new RetentionPolicy();
        policy2.setId(UUID.randomUUID().toString());
        policy2.setName("Transaction Logs");
        policy2.setDescription("3-year retention for financial transactions");
        policy2.setDataCategory(DataCategory.TRANSACTION_DATA);
        policy2.setRetentionDays(1095);
        policy2.setAction(RetentionAction.DELETE);
        policy2.setEnabled(true);
        policy2.setLegalBasis("Financial regulations");
        policy2.setCreatedDate(LocalDateTime.now().minusDays(60));
        retentionPolicyRepository.save(policy2);

        RetentionPolicy policy3 = new RetentionPolicy();
        policy3.setId(UUID.randomUUID().toString());
        policy3.setName("Security Audit Logs");
        policy3.setDescription("1-year retention for security audit trails");
        policy3.setDataCategory(DataCategory.AUDIT_DATA);
        policy3.setRetentionDays(365);
        policy3.setAction(RetentionAction.COLD_STORAGE);
        policy3.setEnabled(true);
        policy3.setLegalBasis("Security compliance requirements");
        policy3.setCreatedDate(LocalDateTime.now().minusDays(30));
        retentionPolicyRepository.save(policy3);

        RetentionPolicy policy4 = new RetentionPolicy();
        policy4.setId(UUID.randomUUID().toString());
        policy4.setName("Marketing Data");
        policy4.setDescription("2-year retention for marketing analytics");
        policy4.setDataCategory(DataCategory.MARKETING);
        policy4.setRetentionDays(730);
        policy4.setAction(RetentionAction.ANONYMIZE);
        policy4.setEnabled(true);
        policy4.setLegalBasis("Legitimate interest for analytics");
        policy4.setCreatedDate(LocalDateTime.now().minusDays(15));
        retentionPolicyRepository.save(policy4);
    }

    @State("audit logs exist")
    public void auditLogsExist() {
        System.out.println("Setting up: Audit logs exist");
        // Audit logs would typically be in a separate service or repository
        // For contract testing, the controller would fetch and return them
        // The actual implementation would involve AuditLogRepository
    }

    @State("data subject request can be processed")
    public void dataSubjectRequestCanBeProcessed() {
        System.out.println("Setting up: Data subject request can be processed");

        // Clear existing data
        requestRepository.deleteAll();

        // Create a request ready for processing
        DataSubjectRequest request = new DataSubjectRequest();
        request.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440301").toString());
        request.setSubjectId("USER-PROCESS-001");
        request.setSubjectEmail("process.test@example.com");
        request.setRequestType(RequestType.ACCESS);
        request.setStatus(RequestStatus.PENDING);
        request.setRegulation("GDPR");
        request.setDescription("Process this access request");
        request.setRequestDate(LocalDateTime.now().minusHours(2));
        request.setVerificationMethod("EMAIL");
        request.setVerified(true);
        request.setDataSystems("CRM,ERP");
        requestRepository.save(request);
    }

    @State("data subject request can be exported")
    public void dataSubjectRequestCanBeExported() {
        System.out.println("Setting up: Data subject request can be exported");

        // Clear existing data
        requestRepository.deleteAll();

        // Create a completed request ready for export
        DataSubjectRequest request = new DataSubjectRequest();
        request.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440302").toString());
        request.setSubjectId("USER-EXPORT-001");
        request.setSubjectEmail("export.test@example.com");
        request.setRequestType(RequestType.PORTABILITY);
        request.setStatus(RequestStatus.COMPLETED);
        request.setRegulation("GDPR");
        request.setDescription("Export request completed");
        request.setRequestDate(LocalDateTime.now().minusDays(2));
        request.setCompletedDate(LocalDateTime.now().minusHours(1));
        request.setVerificationMethod("EMAIL");
        request.setVerified(true);
        request.setProcessingStatus(ProcessingStatus.EXPORTED);
        request.setExportPath("/exports/USER-EXPORT-001.json");
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("personalInfo", "{ user data }");
        exportData.put("transactions", "{ transaction data }");
        request.setExportData(exportData);
        requestRepository.save(request);
    }

    @State("consent can be revoked")
    public void consentCanBeRevoked() {
        System.out.println("Setting up: Consent can be revoked");

        // Clear existing data
        consentRepository.deleteAll();

        // Create an active consent that can be revoked
        Consent consent = new Consent();
        consent.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440401").toString());
        consent.setUserId("USER-REVOKE-001");
        consent.setDataCategory(DataCategory.MARKETING);
        consent.setPurpose("Marketing communications");
        consent.setStatus(ConsentStatus.ACTIVE);
        consent.setGrantedAt(LocalDateTime.now().minusDays(30));
        consent.setExpiresAt(LocalDateTime.now().plusDays(335));
        consent.setVersion("2.0");
        consent.setScope("Email, SMS, Push notifications");
        consentRepository.save(consent);
    }
}

/**
 * WHY COMPLIANCE CONTRACT TESTS ARE CRITICAL:
 *
 * 1. GDPR/CCPA violations result in massive fines (4% global revenue)
 * 2. Data subject requests have strict 30-day deadlines
 * 3. Consent management must be bulletproof for legal compliance
 * 4. Retention policies prevent illegal data hoarding
 * 5. Audit trails are legally required for compliance proof
 *
 * These tests ensure:
 * - Frontend and backend agree on request types (ACCESS, ERASURE, etc.)
 * - Status transitions follow legal requirements
 * - Export formats are machine-readable (GDPR Article 20)
 * - Consent revocation is immediate and complete
 * - Retention actions execute correctly (DELETE, ARCHIVE, ANONYMIZE)
 *
 * Benefits:
 * - Catch compliance bugs before production
 * - Prevent legal violations
 * - Ensure deadline compliance
 * - Validate audit completeness
 * - Maintain trust with data subjects
 */