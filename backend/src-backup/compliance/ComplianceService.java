package com.jivs.platform.service.compliance;

import com.jivs.platform.domain.compliance.*;
import com.jivs.platform.repository.ComplianceRequestRepository;
import com.jivs.platform.repository.ConsentRepository;
import com.jivs.platform.service.retention.RetentionService;
import com.jivs.platform.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for managing GDPR, CCPA and other compliance requirements
 * Handles data subject rights, consent management, and compliance reporting
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceService {

    private final ComplianceRequestRepository complianceRequestRepository;
    private final ConsentRepository consentRepository;
    private final AuditService auditService;
    private final RetentionService retentionService;
    private final StorageService storageService;
    private final DataDiscoveryService dataDiscoveryService;
    private final PIIDetectionService piiDetectionService;

    /**
     * Submit data subject access request (DSAR) - GDPR Article 15, CCPA
     */
    @Transactional
    public ComplianceRequest submitAccessRequest(DataSubjectAccessRequest request) {
        log.info("Processing data subject access request for: {}", request.getEmail());

        // Validate request
        validateAccessRequest(request);

        ComplianceRequest complianceRequest = new ComplianceRequest();
        complianceRequest.setRequestType(ComplianceRequestType.ACCESS);
        complianceRequest.setRegulation(request.getRegulation());
        complianceRequest.setSubjectEmail(request.getEmail());
        complianceRequest.setSubjectIdentifier(request.getIdentifier());
        complianceRequest.setStatus(ComplianceStatus.SUBMITTED);
        complianceRequest.setSubmittedDate(LocalDateTime.now());
        complianceRequest.setDueDate(calculateDueDate(request.getRegulation()));

        ComplianceRequest savedRequest = complianceRequestRepository.save(complianceRequest);

        // Audit the request
        auditService.logEvent("DSAR_SUBMITTED", savedRequest.getId(),
            String.format("Access request submitted for %s under %s",
                request.getEmail(), request.getRegulation()));

        // Trigger async processing
        processAccessRequest(savedRequest.getId());

        log.info("Access request submitted with ID: {}", savedRequest.getId());
        return savedRequest;
    }

    /**
     * Process access request - gather all personal data
     */
    @Async
    @Transactional
    public CompletableFuture<AccessRequestResult> processAccessRequest(Long requestId) {
        log.info("Processing access request: {}", requestId);

        ComplianceRequest request = complianceRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));

        request.setStatus(ComplianceStatus.IN_PROGRESS);
        request.setProcessingStarted(LocalDateTime.now());
        complianceRequestRepository.save(request);

        AccessRequestResult result = new AccessRequestResult();
        result.setRequestId(requestId);

        try {
            // Discover all data related to the subject
            DataDiscoveryResult discovery = dataDiscoveryService.discoverPersonalData(
                request.getSubjectEmail(),
                request.getSubjectIdentifier()
            );

            result.setDataSources(discovery.getSources());
            result.setTotalRecords(discovery.getTotalRecords());

            // Collect data from each source
            Map<String, Object> collectedData = new HashMap<>();
            for (String source : discovery.getSources()) {
                Object data = collectDataFromSource(source, request);
                collectedData.put(source, data);
            }

            result.setCollectedData(collectedData);

            // Generate export package
            String exportPath = generateDataExport(collectedData, request);
            result.setExportPath(exportPath);

            request.setStatus(ComplianceStatus.COMPLETED);
            request.setCompletedDate(LocalDateTime.now());
            request.setResultPath(exportPath);

            // Audit completion
            auditService.logEvent("DSAR_COMPLETED", requestId,
                String.format("Access request completed. Records found: %d", discovery.getTotalRecords()));

            log.info("Access request processed successfully: {}", requestId);

        } catch (Exception e) {
            log.error("Failed to process access request: {}", requestId, e);
            request.setStatus(ComplianceStatus.FAILED);
            request.setErrorMessage(e.getMessage());
            result.setError(e.getMessage());
        }

        complianceRequestRepository.save(request);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Submit right to erasure request (GDPR Article 17)
     */
    @Transactional
    public ComplianceRequest submitErasureRequest(DataErasureRequest request) {
        log.info("Processing erasure request for: {}", request.getEmail());

        validateErasureRequest(request);

        ComplianceRequest complianceRequest = new ComplianceRequest();
        complianceRequest.setRequestType(ComplianceRequestType.ERASURE);
        complianceRequest.setRegulation(Regulation.GDPR);
        complianceRequest.setSubjectEmail(request.getEmail());
        complianceRequest.setSubjectIdentifier(request.getIdentifier());
        complianceRequest.setReason(request.getReason());
        complianceRequest.setStatus(ComplianceStatus.SUBMITTED);
        complianceRequest.setSubmittedDate(LocalDateTime.now());
        complianceRequest.setDueDate(calculateDueDate(Regulation.GDPR));

        ComplianceRequest savedRequest = complianceRequestRepository.save(complianceRequest);

        // Audit the request
        auditService.logEvent("ERASURE_REQUEST_SUBMITTED", savedRequest.getId(),
            String.format("Erasure request submitted for %s", request.getEmail()));

        // Trigger async processing
        processErasureRequest(savedRequest.getId());

        log.info("Erasure request submitted with ID: {}", savedRequest.getId());
        return savedRequest;
    }

    /**
     * Process erasure request - delete/anonymize all personal data
     */
    @Async
    @Transactional
    public CompletableFuture<ErasureRequestResult> processErasureRequest(Long requestId) {
        log.info("Processing erasure request: {}", requestId);

        ComplianceRequest request = complianceRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));

        request.setStatus(ComplianceStatus.IN_PROGRESS);
        request.setProcessingStarted(LocalDateTime.now());
        complianceRequestRepository.save(request);

        ErasureRequestResult result = new ErasureRequestResult();
        result.setRequestId(requestId);

        try {
            // Check for any legal holds
            if (hasLegalHold(request.getSubjectEmail())) {
                throw new ComplianceException("Cannot process erasure - legal hold in effect");
            }

            // Discover all data
            DataDiscoveryResult discovery = dataDiscoveryService.discoverPersonalData(
                request.getSubjectEmail(),
                request.getSubjectIdentifier()
            );

            result.setDataSources(discovery.getSources());
            result.setTotalRecords(discovery.getTotalRecords());

            // Erase data from each source
            int erasedCount = 0;
            for (String source : discovery.getSources()) {
                int count = eraseDataFromSource(source, request);
                erasedCount += count;
            }

            result.setErasedRecords(erasedCount);

            request.setStatus(ComplianceStatus.COMPLETED);
            request.setCompletedDate(LocalDateTime.now());

            // Audit completion
            auditService.logEvent("ERASURE_COMPLETED", requestId,
                String.format("Erasure completed. Records erased: %d", erasedCount));

            log.info("Erasure request processed successfully: {}", requestId);

        } catch (Exception e) {
            log.error("Failed to process erasure request: {}", requestId, e);
            request.setStatus(ComplianceStatus.FAILED);
            request.setErrorMessage(e.getMessage());
            result.setError(e.getMessage());
        }

        complianceRequestRepository.save(request);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Submit right to rectification request (GDPR Article 16)
     */
    @Transactional
    public ComplianceRequest submitRectificationRequest(DataRectificationRequest request) {
        log.info("Processing rectification request for: {}", request.getEmail());

        ComplianceRequest complianceRequest = new ComplianceRequest();
        complianceRequest.setRequestType(ComplianceRequestType.RECTIFICATION);
        complianceRequest.setRegulation(Regulation.GDPR);
        complianceRequest.setSubjectEmail(request.getEmail());
        complianceRequest.setSubjectIdentifier(request.getIdentifier());
        complianceRequest.setCorrections(request.getCorrections());
        complianceRequest.setStatus(ComplianceStatus.SUBMITTED);
        complianceRequest.setSubmittedDate(LocalDateTime.now());
        complianceRequest.setDueDate(calculateDueDate(Regulation.GDPR));

        ComplianceRequest savedRequest = complianceRequestRepository.save(complianceRequest);

        auditService.logEvent("RECTIFICATION_REQUEST_SUBMITTED", savedRequest.getId(),
            "Rectification request submitted");

        processRectificationRequest(savedRequest.getId());

        return savedRequest;
    }

    /**
     * Process rectification request
     */
    @Async
    @Transactional
    public CompletableFuture<RectificationRequestResult> processRectificationRequest(Long requestId) {
        log.info("Processing rectification request: {}", requestId);

        ComplianceRequest request = complianceRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));

        request.setStatus(ComplianceStatus.IN_PROGRESS);
        complianceRequestRepository.save(request);

        RectificationRequestResult result = new RectificationRequestResult();
        result.setRequestId(requestId);

        try {
            // Apply corrections
            Map<String, Object> corrections = request.getCorrections();
            int updatedCount = applyCorrections(request.getSubjectEmail(), corrections);

            result.setUpdatedRecords(updatedCount);
            request.setStatus(ComplianceStatus.COMPLETED);
            request.setCompletedDate(LocalDateTime.now());

            auditService.logEvent("RECTIFICATION_COMPLETED", requestId,
                "Rectification completed. Updated: " + updatedCount);

        } catch (Exception e) {
            log.error("Failed to process rectification request", e);
            request.setStatus(ComplianceStatus.FAILED);
            request.setErrorMessage(e.getMessage());
            result.setError(e.getMessage());
        }

        complianceRequestRepository.save(request);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Submit right to data portability request (GDPR Article 20)
     */
    @Transactional
    public ComplianceRequest submitPortabilityRequest(DataPortabilityRequest request) {
        log.info("Processing portability request for: {}", request.getEmail());

        ComplianceRequest complianceRequest = new ComplianceRequest();
        complianceRequest.setRequestType(ComplianceRequestType.PORTABILITY);
        complianceRequest.setRegulation(Regulation.GDPR);
        complianceRequest.setSubjectEmail(request.getEmail());
        complianceRequest.setExportFormat(request.getFormat());
        complianceRequest.setStatus(ComplianceStatus.SUBMITTED);
        complianceRequest.setSubmittedDate(LocalDateTime.now());
        complianceRequest.setDueDate(calculateDueDate(Regulation.GDPR));

        ComplianceRequest savedRequest = complianceRequestRepository.save(complianceRequest);

        auditService.logEvent("PORTABILITY_REQUEST_SUBMITTED", savedRequest.getId(),
            "Data portability request submitted");

        processPortabilityRequest(savedRequest.getId());

        return savedRequest;
    }

    /**
     * Manage consent (GDPR Article 7)
     */
    @Transactional
    public Consent recordConsent(ConsentRequest request) {
        log.info("Recording consent for: {} - {}", request.getSubjectEmail(), request.getPurpose());

        Consent consent = new Consent();
        consent.setSubjectEmail(request.getSubjectEmail());
        consent.setPurpose(request.getPurpose());
        consent.setConsentGiven(request.isConsentGiven());
        consent.setConsentDate(LocalDateTime.now());
        consent.setConsentMethod(request.getConsentMethod());
        consent.setLegalBasis(request.getLegalBasis());
        consent.setExpiryDate(request.getExpiryDate());
        consent.setActive(true);

        Consent savedConsent = consentRepository.save(consent);

        auditService.logEvent("CONSENT_RECORDED", savedConsent.getId(),
            String.format("Consent %s for purpose: %s",
                request.isConsentGiven() ? "given" : "withdrawn",
                request.getPurpose()));

        return savedConsent;
    }

    /**
     * Withdraw consent
     */
    @Transactional
    public void withdrawConsent(String subjectEmail, String purpose) {
        log.info("Withdrawing consent for: {} - {}", subjectEmail, purpose);

        List<Consent> consents = consentRepository.findBySubjectEmailAndPurposeAndActive(
            subjectEmail, purpose, true
        );

        for (Consent consent : consents) {
            consent.setActive(false);
            consent.setWithdrawnDate(LocalDateTime.now());
            consentRepository.save(consent);
        }

        auditService.logEvent("CONSENT_WITHDRAWN", null,
            String.format("Consent withdrawn for %s - %s", subjectEmail, purpose));

        // Trigger data processing review
        reviewDataProcessing(subjectEmail, purpose);
    }

    /**
     * Generate compliance report
     */
    public ComplianceReport generateComplianceReport(ComplianceReportRequest request) {
        log.info("Generating compliance report for period: {} to {}",
            request.getStartDate(), request.getEndDate());

        ComplianceReport report = new ComplianceReport();
        report.setStartDate(request.getStartDate());
        report.setEndDate(request.getEndDate());
        report.setRegulation(request.getRegulation());
        report.setGeneratedDate(LocalDateTime.now());

        // Request statistics
        List<ComplianceRequest> requests = complianceRequestRepository
            .findBySubmittedDateBetween(request.getStartDate(), request.getEndDate());

        report.setTotalRequests(requests.size());
        report.setRequestsByType(groupByType(requests));
        report.setRequestsByStatus(groupByStatus(requests));

        // SLA compliance
        long withinSLA = requests.stream()
            .filter(this::isWithinSLA)
            .count();
        report.setSlaCompliance((double) withinSLA / requests.size() * 100);

        // Consent statistics
        List<Consent> consents = consentRepository
            .findByConsentDateBetween(request.getStartDate(), request.getEndDate());

        report.setTotalConsents(consents.size());
        report.setConsentsGiven(consents.stream().filter(Consent::isConsentGiven).count());
        report.setConsentsWithdrawn(consents.stream()
            .filter(c -> !c.isConsentGiven())
            .count());

        // Data breach incidents
        report.setDataBreaches(getDataBreaches(request.getStartDate(), request.getEndDate()));

        // Recommendations
        report.setRecommendations(generateRecommendations(report));

        log.info("Compliance report generated");
        return report;
    }

    /**
     * Scheduled compliance scan
     */
    @Scheduled(cron = "0 0 1 * * ?") // Run at 1 AM daily
    @Transactional
    public void scheduledComplianceScan() {
        log.info("Starting scheduled compliance scan");

        try {
            // Check for overdue requests
            List<ComplianceRequest> overdueRequests = complianceRequestRepository
                .findByStatusAndDueDateBefore(ComplianceStatus.IN_PROGRESS, LocalDateTime.now());

            for (ComplianceRequest request : overdueRequests) {
                log.warn("Overdue compliance request: {} - {}", request.getId(), request.getRequestType());
                // Send notifications
            }

            // Check for expired consents
            List<Consent> expiredConsents = consentRepository
                .findByActiveAndExpiryDateBefore(true, LocalDateTime.now());

            for (Consent consent : expiredConsents) {
                consent.setActive(false);
                consentRepository.save(consent);
                log.info("Expired consent deactivated: {}", consent.getId());
            }

            // Detect unauthorized PII exposure
            detectUnauthorizedPII();

            log.info("Scheduled compliance scan completed");

        } catch (Exception e) {
            log.error("Compliance scan failed", e);
        }
    }

    /**
     * Detect PII in unprotected locations
     */
    private void detectUnauthorizedPII() {
        // Scan for PII in logs, temporary storage, etc.
        List<PIIDetection> detections = piiDetectionService.scanForUnauthorizedPII();

        for (PIIDetection detection : detections) {
            log.warn("Unauthorized PII detected: {} in {}", detection.getPiiType(), detection.getLocation());

            auditService.logEvent("UNAUTHORIZED_PII_DETECTED", null,
                String.format("PII type: %s, Location: %s", detection.getPiiType(), detection.getLocation()));

            // Take remediation action
            remediatePIIExposure(detection);
        }
    }

    // Helper methods
    private void validateAccessRequest(DataSubjectAccessRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Subject email is required");
        }
        if (request.getRegulation() == null) {
            throw new IllegalArgumentException("Regulation must be specified");
        }
    }

    private void validateErasureRequest(DataErasureRequest request) {
        if (request.getEmail() == null) {
            throw new IllegalArgumentException("Subject email is required");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Reason for erasure is required");
        }
    }

    private LocalDateTime calculateDueDate(Regulation regulation) {
        // GDPR: 30 days, CCPA: 45 days
        int days = regulation == Regulation.GDPR ? 30 : 45;
        return LocalDateTime.now().plusDays(days);
    }

    private boolean hasLegalHold(String email) {
        // Check if subject has legal hold
        return false; // Simplified
    }

    private Object collectDataFromSource(String source, ComplianceRequest request) {
        // Collect data from specific source
        return new HashMap<>(); // Simplified
    }

    private int eraseDataFromSource(String source, ComplianceRequest request) {
        // Erase/anonymize data from source
        return 0; // Simplified
    }

    private int applyCorrections(String email, Map<String, Object> corrections) {
        // Apply data corrections
        return corrections.size(); // Simplified
    }

    private String generateDataExport(Map<String, Object> data, ComplianceRequest request) {
        // Generate export file in requested format
        return storageService.storeDataExport(data, request.getExportFormat());
    }

    private void reviewDataProcessing(String email, String purpose) {
        // Review if any data processing should be stopped
        log.info("Reviewing data processing for {} - {}", email, purpose);
    }

    private Map<ComplianceRequestType, Long> groupByType(List<ComplianceRequest> requests) {
        return requests.stream()
            .collect(Collectors.groupingBy(
                ComplianceRequest::getRequestType,
                Collectors.counting()
            ));
    }

    private Map<ComplianceStatus, Long> groupByStatus(List<ComplianceRequest> requests) {
        return requests.stream()
            .collect(Collectors.groupingBy(
                ComplianceRequest::getStatus,
                Collectors.counting()
            ));
    }

    private boolean isWithinSLA(ComplianceRequest request) {
        if (request.getCompletedDate() == null) {
            return false;
        }
        return request.getCompletedDate().isBefore(request.getDueDate());
    }

    private List<DataBreachIncident> getDataBreaches(LocalDateTime start, LocalDateTime end) {
        // Get data breach incidents
        return new ArrayList<>(); // Simplified
    }

    private List<String> generateRecommendations(ComplianceReport report) {
        List<String> recommendations = new ArrayList<>();

        if (report.getSlaCompliance() < 90) {
            recommendations.add("Improve SLA compliance - currently at " + report.getSlaCompliance() + "%");
        }

        if (report.getDataBreaches().size() > 0) {
            recommendations.add("Review security measures - " + report.getDataBreaches().size() + " breaches detected");
        }

        return recommendations;
    }

    private void remediatePIIExposure(PIIDetection detection) {
        // Remediate PII exposure
        log.info("Remediating PII exposure at: {}", detection.getLocation());
    }

    @Async
    @Transactional
    public CompletableFuture<PortabilityRequestResult> processPortabilityRequest(Long requestId) {
        // Process portability request
        return CompletableFuture.completedFuture(new PortabilityRequestResult());
    }
}

/**
 * Compliance exception
 */
class ComplianceException extends RuntimeException {
    public ComplianceException(String message) {
        super(message);
    }
}