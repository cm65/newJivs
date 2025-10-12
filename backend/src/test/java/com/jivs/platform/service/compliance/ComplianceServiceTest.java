package com.jivs.platform.service.compliance;

import com.jivs.platform.domain.DataSubjectRequest;
import com.jivs.platform.domain.ConsentRecord;
import com.jivs.platform.repository.DataSubjectRequestRepository;
import com.jivs.platform.repository.ConsentRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplianceServiceTest {

    @Mock
    private DataSubjectRequestRepository requestRepository;

    @Mock
    private ConsentRecordRepository consentRepository;

    @Mock
    private DataDiscoveryService dataDiscoveryService;

    @Mock
    private PIIDetectionService piiDetectionService;

    @InjectMocks
    private ComplianceService complianceService;

    private DataSubjectRequest testRequest;
    private ConsentRecord testConsent;

    @BeforeEach
    void setUp() {
        testRequest = new DataSubjectRequest();
        testRequest.setId(UUID.randomUUID().toString());
        testRequest.setRequestType("ACCESS");
        testRequest.setRegulation("GDPR");
        testRequest.setDataSubjectId("user123");
        testRequest.setDataSubjectEmail("user@example.com");
        testRequest.setStatus("PENDING");
        testRequest.setPriority("MEDIUM");
        testRequest.setRequestDetails("Request to access personal data");
        testRequest.setRequestedAt(new Date());
        testRequest.setDueDate(new Date(System.currentTimeMillis() + 2592000000L)); // 30 days
        testRequest.setCreatedAt(new Date());

        testConsent = new ConsentRecord();
        testConsent.setId(UUID.randomUUID().toString());
        testConsent.setDataSubjectId("user123");
        testConsent.setDataSubjectEmail("user@example.com");
        testConsent.setConsentType("MARKETING");
        testConsent.setPurpose("Marketing communications");
        testConsent.setGranted(true);
        testConsent.setGrantedAt(new Date());
        testConsent.setLegalBasis("CONSENT");
        testConsent.setVersion("1.0");
        testConsent.setCreatedAt(new Date());
    }

    @Test
    void testCreateRequest_Success() {
        // Arrange
        when(requestRepository.save(any(DataSubjectRequest.class))).thenReturn(testRequest);

        // Act
        DataSubjectRequest result = complianceService.createRequest(testRequest);

        // Assert
        assertNotNull(result);
        assertEquals("ACCESS", result.getRequestType());
        assertEquals("GDPR", result.getRegulation());
        assertEquals("PENDING", result.getStatus());
        verify(requestRepository, times(1)).save(any(DataSubjectRequest.class));
    }

    @Test
    void testCreateRequest_NullRequest_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            complianceService.createRequest(null)
        );
        verify(requestRepository, never()).save(any(DataSubjectRequest.class));
    }

    @Test
    void testGetRequest_Success() {
        // Arrange
        when(requestRepository.findById(testRequest.getId()))
            .thenReturn(Optional.of(testRequest));

        // Act
        Optional<DataSubjectRequest> result = complianceService.getRequest(testRequest.getId());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testRequest.getId(), result.get().getId());
        verify(requestRepository, times(1)).findById(testRequest.getId());
    }

    @Test
    void testGetRequest_NotFound() {
        // Arrange
        String nonExistentId = UUID.randomUUID().toString();
        when(requestRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act
        Optional<DataSubjectRequest> result = complianceService.getRequest(nonExistentId);

        // Assert
        assertFalse(result.isPresent());
        verify(requestRepository, times(1)).findById(nonExistentId);
    }

    @Test
    void testGetAllRequests_Success() {
        // Arrange
        List<DataSubjectRequest> requests = Arrays.asList(testRequest);
        Page<DataSubjectRequest> page = new PageImpl<>(requests);
        Pageable pageable = PageRequest.of(0, 20);

        when(requestRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<DataSubjectRequest> result = complianceService.getAllRequests(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testRequest.getId(), result.getContent().get(0).getId());
        verify(requestRepository, times(1)).findAll(pageable);
    }

    @Test
    void testGetRequestsByStatus_Success() {
        // Arrange
        List<DataSubjectRequest> requests = Arrays.asList(testRequest);
        Page<DataSubjectRequest> page = new PageImpl<>(requests);
        Pageable pageable = PageRequest.of(0, 20);

        when(requestRepository.findByStatus("PENDING", pageable)).thenReturn(page);

        // Act
        Page<DataSubjectRequest> result = complianceService.getRequestsByStatus("PENDING", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("PENDING", result.getContent().get(0).getStatus());
        verify(requestRepository, times(1)).findByStatus("PENDING", pageable);
    }

    @Test
    void testGetRequestsByType_Success() {
        // Arrange
        List<DataSubjectRequest> requests = Arrays.asList(testRequest);
        Page<DataSubjectRequest> page = new PageImpl<>(requests);
        Pageable pageable = PageRequest.of(0, 20);

        when(requestRepository.findByRequestType("ACCESS", pageable)).thenReturn(page);

        // Act
        Page<DataSubjectRequest> result = complianceService.getRequestsByType("ACCESS", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("ACCESS", result.getContent().get(0).getRequestType());
        verify(requestRepository, times(1)).findByRequestType("ACCESS", pageable);
    }

    @Test
    void testProcessRequest_AccessRequest_Success() {
        // Arrange
        testRequest.setRequestType("ACCESS");
        when(requestRepository.findById(testRequest.getId()))
            .thenReturn(Optional.of(testRequest));
        when(requestRepository.save(any(DataSubjectRequest.class))).thenReturn(testRequest);
        when(dataDiscoveryService.discoverPersonalData(testRequest.getDataSubjectId()))
            .thenReturn(new HashMap<>());

        // Act
        complianceService.processRequest(testRequest.getId());

        // Assert
        verify(requestRepository, times(1)).findById(testRequest.getId());
        verify(dataDiscoveryService, times(1)).discoverPersonalData(testRequest.getDataSubjectId());
        verify(requestRepository, atLeastOnce()).save(any(DataSubjectRequest.class));
    }

    @Test
    void testProcessRequest_ErasureRequest_Success() {
        // Arrange
        testRequest.setRequestType("ERASURE");
        when(requestRepository.findById(testRequest.getId()))
            .thenReturn(Optional.of(testRequest));
        when(requestRepository.save(any(DataSubjectRequest.class))).thenReturn(testRequest);

        // Act
        complianceService.processRequest(testRequest.getId());

        // Assert
        verify(requestRepository, times(1)).findById(testRequest.getId());
        verify(requestRepository, atLeastOnce()).save(any(DataSubjectRequest.class));
    }

    @Test
    void testProcessRequest_NotFound_ThrowsException() {
        // Arrange
        String nonExistentId = UUID.randomUUID().toString();
        when(requestRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            complianceService.processRequest(nonExistentId)
        );
    }

    @Test
    void testExportRequestData_Success() {
        // Arrange
        testRequest.setRequestType("ACCESS");
        testRequest.setStatus("COMPLETED");
        Map<String, Object> personalData = new HashMap<>();
        personalData.put("name", "John Doe");
        personalData.put("email", "john@example.com");
        testRequest.setResponseData(personalData);

        when(requestRepository.findById(testRequest.getId()))
            .thenReturn(Optional.of(testRequest));

        // Act
        Map<String, Object> result = complianceService.exportRequestData(testRequest.getId(), "JSON");

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("name"));
        assertEquals("John Doe", result.get("name"));
        verify(requestRepository, times(1)).findById(testRequest.getId());
    }

    @Test
    void testExportRequestData_NotAccessRequest_ThrowsException() {
        // Arrange
        testRequest.setRequestType("ERASURE");
        when(requestRepository.findById(testRequest.getId()))
            .thenReturn(Optional.of(testRequest));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            complianceService.exportRequestData(testRequest.getId(), "JSON")
        );
    }

    @Test
    void testCreateConsent_Success() {
        // Arrange
        when(consentRepository.save(any(ConsentRecord.class))).thenReturn(testConsent);

        // Act
        ConsentRecord result = complianceService.createConsent(testConsent);

        // Assert
        assertNotNull(result);
        assertEquals("MARKETING", result.getConsentType());
        assertTrue(result.isGranted());
        verify(consentRepository, times(1)).save(any(ConsentRecord.class));
    }

    @Test
    void testRevokeConsent_Success() {
        // Arrange
        when(consentRepository.findById(testConsent.getId()))
            .thenReturn(Optional.of(testConsent));
        when(consentRepository.save(any(ConsentRecord.class))).thenReturn(testConsent);

        // Act
        complianceService.revokeConsent(testConsent.getId());

        // Assert
        verify(consentRepository, times(1)).findById(testConsent.getId());
        verify(consentRepository, times(1)).save(any(ConsentRecord.class));
    }

    @Test
    void testRevokeConsent_NotFound_ThrowsException() {
        // Arrange
        String nonExistentId = UUID.randomUUID().toString();
        when(consentRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            complianceService.revokeConsent(nonExistentId)
        );
    }

    @Test
    void testGetConsentsByDataSubject_Success() {
        // Arrange
        List<ConsentRecord> consents = Arrays.asList(testConsent);
        when(consentRepository.findByDataSubjectId("user123")).thenReturn(consents);

        // Act
        List<ConsentRecord> result = complianceService.getConsentsByDataSubject("user123");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("user123", result.get(0).getDataSubjectId());
        verify(consentRepository, times(1)).findByDataSubjectId("user123");
    }

    @Test
    void testGetActiveConsents_Success() {
        // Arrange
        List<ConsentRecord> consents = Arrays.asList(testConsent);
        when(consentRepository.findByGranted(true)).thenReturn(consents);

        // Act
        List<ConsentRecord> result = complianceService.getActiveConsents();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isGranted());
        verify(consentRepository, times(1)).findByGranted(true);
    }

    @Test
    void testCheckConsentExpiry() {
        // Arrange
        Date pastDate = new Date(System.currentTimeMillis() - 86400000); // 1 day ago
        testConsent.setExpiresAt(pastDate);
        List<ConsentRecord> expiredConsents = Arrays.asList(testConsent);

        when(consentRepository.findByGrantedAndExpiresAtBefore(eq(true), any(Date.class)))
            .thenReturn(expiredConsents);
        when(consentRepository.save(any(ConsentRecord.class))).thenReturn(testConsent);

        // Act
        complianceService.checkConsentExpiry();

        // Assert
        verify(consentRepository, times(1))
            .findByGrantedAndExpiresAtBefore(eq(true), any(Date.class));
        verify(consentRepository, times(1)).save(any(ConsentRecord.class));
    }

    @Test
    void testGetDashboardMetrics() {
        // Arrange
        when(requestRepository.count()).thenReturn(50L);
        when(requestRepository.countByStatus("PENDING")).thenReturn(15L);
        when(requestRepository.countByOverdueRequests(any(Date.class))).thenReturn(5L);
        when(requestRepository.countByStatus("COMPLETED")).thenReturn(30L);
        when(consentRepository.count()).thenReturn(100L);
        when(consentRepository.countByGranted(true)).thenReturn(75L);
        when(consentRepository.countByGranted(false)).thenReturn(25L);

        // Act
        Map<String, Object> metrics = complianceService.getDashboardMetrics();

        // Assert
        assertNotNull(metrics);
        assertEquals(50L, metrics.get("totalRequests"));
        assertEquals(15L, metrics.get("pendingRequests"));
        assertEquals(5L, metrics.get("overdueRequests"));
        assertEquals(100L, metrics.get("totalConsents"));
    }

    @Test
    void testCalculateDueDate_GDPR() {
        // Act
        Date dueDate = complianceService.calculateDueDate("GDPR", "ACCESS");

        // Assert
        assertNotNull(dueDate);
        // GDPR Article 15 requests should be completed within 30 days
        long daysDifference = (dueDate.getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24);
        assertEquals(30, daysDifference, 1); // Allow 1 day tolerance
    }

    @Test
    void testCalculateDueDate_CCPA() {
        // Act
        Date dueDate = complianceService.calculateDueDate("CCPA", "ACCESS");

        // Assert
        assertNotNull(dueDate);
        // CCPA requests should be completed within 45 days
        long daysDifference = (dueDate.getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24);
        assertEquals(45, daysDifference, 1); // Allow 1 day tolerance
    }

    @Test
    void testCountRequestsByType() {
        // Arrange
        when(requestRepository.countByRequestType("ACCESS")).thenReturn(20L);

        // Act
        long result = complianceService.countRequestsByType("ACCESS");

        // Assert
        assertEquals(20L, result);
        verify(requestRepository, times(1)).countByRequestType("ACCESS");
    }

    @Test
    void testCountRequestsByRegulation() {
        // Arrange
        when(requestRepository.countByRegulation("GDPR")).thenReturn(35L);

        // Act
        long result = complianceService.countRequestsByRegulation("GDPR");

        // Assert
        assertEquals(35L, result);
        verify(requestRepository, times(1)).countByRegulation("GDPR");
    }

    @Test
    void testGetOverdueRequests() {
        // Arrange
        List<DataSubjectRequest> requests = Arrays.asList(testRequest);
        when(requestRepository.findOverdueRequests(any(Date.class))).thenReturn(requests);

        // Act
        List<DataSubjectRequest> result = complianceService.getOverdueRequests();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(requestRepository, times(1)).findOverdueRequests(any(Date.class));
    }

    @Test
    void testUpdateRequestStatus() {
        // Arrange
        when(requestRepository.findById(testRequest.getId()))
            .thenReturn(Optional.of(testRequest));
        when(requestRepository.save(any(DataSubjectRequest.class))).thenReturn(testRequest);

        // Act
        complianceService.updateRequestStatus(testRequest.getId(), "IN_PROGRESS");

        // Assert
        verify(requestRepository, times(1)).findById(testRequest.getId());
        verify(requestRepository, times(1)).save(any(DataSubjectRequest.class));
    }

    @Test
    void testValidateRequest_Valid() {
        // Arrange
        testRequest.setDataSubjectEmail("valid@example.com");
        testRequest.setRequestDetails("Valid request details");

        // Act
        boolean result = complianceService.validateRequest(testRequest);

        // Assert
        assertTrue(result);
    }

    @Test
    void testValidateRequest_InvalidEmail() {
        // Arrange
        testRequest.setDataSubjectEmail("invalid-email");

        // Act
        boolean result = complianceService.validateRequest(testRequest);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateRequest_MissingDetails() {
        // Arrange
        testRequest.setRequestDetails("");

        // Act
        boolean result = complianceService.validateRequest(testRequest);

        // Assert
        assertFalse(result);
    }

    @Test
    void testGetRecentRequests() {
        // Arrange
        List<DataSubjectRequest> requests = Arrays.asList(testRequest);
        when(requestRepository.findTop10ByOrderByRequestedAtDesc()).thenReturn(requests);

        // Act
        List<DataSubjectRequest> result = complianceService.getRecentRequests();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(requestRepository, times(1)).findTop10ByOrderByRequestedAtDesc();
    }
}
