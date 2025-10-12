package com.jivs.platform.service.compliance;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for detecting Personally Identifiable Information (PII) in various locations
 * Essential for GDPR/CCPA compliance and data protection
 */
@Service
@RequiredArgsConstructor
public class PIIDetectionService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PIIDetectionService.class);

    // PII Patterns
    private static final Map<String, Pattern> PII_PATTERNS = new HashMap<>();

    static {
        // Email pattern
        PII_PATTERNS.put("EMAIL", Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"
        ));

        // US Phone number
        PII_PATTERNS.put("US_PHONE", Pattern.compile(
            "\\b(?:\\+?1[-.]?)?\\(?([0-9]{3})\\)?[-.]?([0-9]{3})[-.]?([0-9]{4})\\b"
        ));

        // Social Security Number
        PII_PATTERNS.put("SSN", Pattern.compile(
            "\\b(?!000|666)[0-9]{3}-(?!00)[0-9]{2}-(?!0000)[0-9]{4}\\b"
        ));

        // Credit Card (simplified)
        PII_PATTERNS.put("CREDIT_CARD", Pattern.compile(
            "\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|6(?:011|5[0-9]{2})[0-9]{12})\\b"
        ));

        // IP Address
        PII_PATTERNS.put("IP_ADDRESS", Pattern.compile(
            "\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"
        ));

        // Date of Birth (various formats)
        PII_PATTERNS.put("DATE_OF_BIRTH", Pattern.compile(
            "\\b(?:0[1-9]|1[0-2])[/\\-](?:0[1-9]|[12][0-9]|3[01])[/\\-](?:19|20)\\d{2}\\b"
        ));

        // US Passport
        PII_PATTERNS.put("US_PASSPORT", Pattern.compile(
            "\\b[A-Z][0-9]{8}\\b"
        ));

        // Driver's License (varies by state - simplified)
        PII_PATTERNS.put("DRIVERS_LICENSE", Pattern.compile(
            "\\b[A-Z]{1,2}[0-9]{5,8}\\b"
        ));

        // Bank Account (IBAN)
        PII_PATTERNS.put("IBAN", Pattern.compile(
            "\\b[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}\\b"
        ));
    }

    /**
     * Scan for unauthorized PII in various locations
     */
    public List<PIIDetection> scanForUnauthorizedPII() {
        log.info("Starting unauthorized PII scan");

        List<PIIDetection> detections = new ArrayList<>();

        // Scan log files
        detections.addAll(scanLogFiles());

        // Scan temp directories
        detections.addAll(scanTempDirectories());

        // Scan database exports
        detections.addAll(scanDatabaseExports());

        // Scan unencrypted files
        detections.addAll(scanUnencryptedFiles());

        // Scan public directories
        detections.addAll(scanPublicDirectories());

        log.info("PII scan completed. Found {} potential PII exposures", detections.size());
        return detections;
    }

    /**
     * Detect PII in text content
     */
    public List<PIIMatch> detectPII(String text) {
        List<PIIMatch> matches = new ArrayList<>();

        for (Map.Entry<String, Pattern> entry : PII_PATTERNS.entrySet()) {
            String piiType = entry.getKey();
            Pattern pattern = entry.getValue();

            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                PIIMatch match = new PIIMatch();
                match.setPiiType(piiType);
                match.setValue(maskPII(matcher.group(), piiType));
                match.setStartPosition(matcher.start());
                match.setEndPosition(matcher.end());
                match.setConfidence(calculateConfidence(piiType, matcher.group()));
                matches.add(match);
            }
        }

        return matches;
    }

    /**
     * Scan log files for PII
     */
    private List<PIIDetection> scanLogFiles() {
        List<PIIDetection> detections = new ArrayList<>();

        List<String> logPaths = Arrays.asList(
            "/var/log/application",
            "/logs",
            "./logs"
        );

        for (String logPath : logPaths) {
            try {
                Path path = Paths.get(logPath);
                if (Files.exists(path) && Files.isDirectory(path)) {
                    Files.walk(path)
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".log"))
                        .forEach(file -> {
                            List<PIIDetection> fileDetections = scanFile(file.toFile(), "LOG_FILE");
                            detections.addAll(fileDetections);
                        });
                }
            } catch (IOException e) {
                log.error("Error scanning log path {}: {}", logPath, e.getMessage());
            }
        }

        return detections;
    }

    /**
     * Scan temporary directories for PII
     */
    private List<PIIDetection> scanTempDirectories() {
        List<PIIDetection> detections = new ArrayList<>();

        List<String> tempPaths = Arrays.asList(
            System.getProperty("java.io.tmpdir"),
            "/tmp",
            "/var/tmp",
            "./temp"
        );

        for (String tempPath : tempPaths) {
            try {
                Path path = Paths.get(tempPath);
                if (Files.exists(path) && Files.isDirectory(path)) {
                    Files.walk(path, 1) // Only scan one level deep
                        .filter(Files::isRegularFile)
                        .forEach(file -> {
                            List<PIIDetection> fileDetections = scanFile(file.toFile(), "TEMP_FILE");
                            detections.addAll(fileDetections);
                        });
                }
            } catch (IOException e) {
                log.error("Error scanning temp path {}: {}", tempPath, e.getMessage());
            }
        }

        return detections;
    }

    /**
     * Scan database export files for PII
     */
    private List<PIIDetection> scanDatabaseExports() {
        List<PIIDetection> detections = new ArrayList<>();

        List<String> exportPaths = Arrays.asList(
            "/data/exports",
            "/backups",
            "./exports"
        );

        for (String exportPath : exportPaths) {
            try {
                Path path = Paths.get(exportPath);
                if (Files.exists(path) && Files.isDirectory(path)) {
                    Files.walk(path)
                        .filter(Files::isRegularFile)
                        .filter(p -> {
                            String name = p.toString().toLowerCase();
                            return name.endsWith(".sql") ||
                                   name.endsWith(".csv") ||
                                   name.endsWith(".dump");
                        })
                        .forEach(file -> {
                            List<PIIDetection> fileDetections = scanFile(file.toFile(), "DATABASE_EXPORT");
                            detections.addAll(fileDetections);
                        });
                }
            } catch (IOException e) {
                log.error("Error scanning export path {}: {}", exportPath, e.getMessage());
            }
        }

        return detections;
    }

    /**
     * Scan unencrypted files for PII
     */
    private List<PIIDetection> scanUnencryptedFiles() {
        List<PIIDetection> detections = new ArrayList<>();

        // Check for unencrypted files that should be encrypted
        List<String> sensitivePaths = Arrays.asList(
            "/data/sensitive",
            "/config",
            "./config"
        );

        for (String sensitivePath : sensitivePaths) {
            try {
                Path path = Paths.get(sensitivePath);
                if (Files.exists(path) && Files.isDirectory(path)) {
                    Files.walk(path)
                        .filter(Files::isRegularFile)
                        .filter(p -> !isEncrypted(p))
                        .forEach(file -> {
                            List<PIIDetection> fileDetections = scanFile(file.toFile(), "UNENCRYPTED_FILE");
                            detections.addAll(fileDetections);
                        });
                }
            } catch (IOException e) {
                log.error("Error scanning sensitive path {}: {}", sensitivePath, e.getMessage());
            }
        }

        return detections;
    }

    /**
     * Scan public directories for PII
     */
    private List<PIIDetection> scanPublicDirectories() {
        List<PIIDetection> detections = new ArrayList<>();

        List<String> publicPaths = Arrays.asList(
            "/var/www/html",
            "/public",
            "./public",
            "./static"
        );

        for (String publicPath : publicPaths) {
            try {
                Path path = Paths.get(publicPath);
                if (Files.exists(path) && Files.isDirectory(path)) {
                    Files.walk(path)
                        .filter(Files::isRegularFile)
                        .forEach(file -> {
                            List<PIIDetection> fileDetections = scanFile(file.toFile(), "PUBLIC_FILE");
                            detections.addAll(fileDetections);
                        });
                }
            } catch (IOException e) {
                log.error("Error scanning public path {}: {}", publicPath, e.getMessage());
            }
        }

        return detections;
    }

    /**
     * Scan a single file for PII
     */
    private List<PIIDetection> scanFile(File file, String locationType) {
        List<PIIDetection> detections = new ArrayList<>();

        try {
            // Skip large files (> 10MB)
            if (file.length() > 10 * 1024 * 1024) {
                log.debug("Skipping large file: {}", file.getPath());
                return detections;
            }

            String content = new String(Files.readAllBytes(file.toPath()));
            List<PIIMatch> matches = detectPII(content);

            for (PIIMatch match : matches) {
                PIIDetection detection = new PIIDetection();
                detection.setPiiType(match.getPiiType());
                detection.setLocation(file.getAbsolutePath());
                detection.setLocationType(locationType);
                detection.setValue(match.getValue());
                detection.setLineNumber(getLineNumber(content, match.getStartPosition()));
                detection.setSeverity(determinePIISeverity(match.getPiiType()));
                detection.setDetectionTime(new Date());
                detection.setRemediationRequired(true);
                detections.add(detection);
            }

        } catch (IOException e) {
            log.error("Error scanning file {}: {}", file.getPath(), e.getMessage());
        }

        return detections;
    }

    /**
     * Check if a file is encrypted
     */
    private boolean isEncrypted(Path path) {
        // Check file headers or extensions for encryption
        String filename = path.getFileName().toString();
        return filename.endsWith(".enc") ||
               filename.endsWith(".encrypted") ||
               filename.endsWith(".gpg") ||
               filename.endsWith(".aes");
    }

    /**
     * Mask PII value for logging/reporting
     */
    private String maskPII(String value, String piiType) {
        if (value == null || value.length() < 4) {
            return "***";
        }

        switch (piiType) {
            case "EMAIL":
                int atIndex = value.indexOf('@');
                if (atIndex > 2) {
                    return value.substring(0, 2) + "***" + value.substring(atIndex);
                }
                break;
            case "SSN":
                return "XXX-XX-" + value.substring(value.length() - 4);
            case "CREDIT_CARD":
                return "**** **** **** " + value.substring(value.length() - 4);
            case "US_PHONE":
                return "***-***-" + value.substring(value.length() - 4);
            default:
                // Generic masking
                int showChars = Math.min(3, value.length() / 3);
                return value.substring(0, showChars) + "***" +
                       (value.length() > showChars * 2 ?
                        value.substring(value.length() - showChars) : "");
        }

        return "***";
    }

    /**
     * Calculate confidence score for PII detection
     */
    private double calculateConfidence(String piiType, String value) {
        // Implement confidence scoring based on pattern strength and context
        switch (piiType) {
            case "SSN":
            case "CREDIT_CARD":
                return 0.95; // High confidence for structured data
            case "EMAIL":
            case "IP_ADDRESS":
                return 0.90;
            case "US_PHONE":
                return 0.85;
            case "DATE_OF_BIRTH":
                return 0.70; // Lower confidence as dates can be other things
            default:
                return 0.60;
        }
    }

    /**
     * Determine severity of PII exposure
     */
    private String determinePIISeverity(String piiType) {
        switch (piiType) {
            case "SSN":
            case "CREDIT_CARD":
            case "US_PASSPORT":
            case "IBAN":
                return "CRITICAL";
            case "DATE_OF_BIRTH":
            case "DRIVERS_LICENSE":
                return "HIGH";
            case "EMAIL":
            case "US_PHONE":
                return "MEDIUM";
            case "IP_ADDRESS":
                return "LOW";
            default:
                return "INFO";
        }
    }

    /**
     * Get line number for a position in text
     */
    private int getLineNumber(String text, int position) {
        int lineNumber = 1;
        for (int i = 0; i < position && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lineNumber++;
            }
        }
        return lineNumber;
    }

    /**
     * Generate PII detection report
     */
    public PIIDetectionReport generateReport(List<PIIDetection> detections) {
        PIIDetectionReport report = new PIIDetectionReport();
        report.setReportDate(new Date());
        report.setTotalDetections(detections.size());

        // Group by PII type
        Map<String, Long> byType = detections.stream()
            .collect(Collectors.groupingBy(PIIDetection::getPiiType, Collectors.counting()));
        report.setDetectionsByType(byType);

        // Group by location type
        Map<String, Long> byLocation = detections.stream()
            .collect(Collectors.groupingBy(PIIDetection::getLocationType, Collectors.counting()));
        report.setDetectionsByLocation(byLocation);

        // Group by severity
        Map<String, Long> bySeverity = detections.stream()
            .collect(Collectors.groupingBy(PIIDetection::getSeverity, Collectors.counting()));
        report.setDetectionsBySeverity(bySeverity);

        // Critical detections
        List<PIIDetection> criticalDetections = detections.stream()
            .filter(d -> "CRITICAL".equals(d.getSeverity()))
            .collect(Collectors.toList());
        report.setCriticalDetections(criticalDetections);

        // Remediation recommendations
        report.setRecommendations(generateRecommendations(detections));

        return report;
    }

    /**
     * Generate remediation recommendations
     */
    private List<String> generateRecommendations(List<PIIDetection> detections) {
        Set<String> recommendations = new HashSet<>();

        for (PIIDetection detection : detections) {
            switch (detection.getLocationType()) {
                case "LOG_FILE":
                    recommendations.add("Implement PII scrubbing in logging framework");
                    recommendations.add("Review and update logging policies");
                    break;
                case "TEMP_FILE":
                    recommendations.add("Implement automatic cleanup of temporary files");
                    recommendations.add("Encrypt temporary files containing sensitive data");
                    break;
                case "DATABASE_EXPORT":
                    recommendations.add("Encrypt all database exports");
                    recommendations.add("Implement secure export procedures");
                    break;
                case "UNENCRYPTED_FILE":
                    recommendations.add("Enable encryption at rest for sensitive files");
                    recommendations.add("Review file encryption policies");
                    break;
                case "PUBLIC_FILE":
                    recommendations.add("Remove PII from public directories immediately");
                    recommendations.add("Implement access controls on public directories");
                    break;
            }
        }

        return new ArrayList<>(recommendations);
    }
}

/**
 * PII detection result
 */
class PIIDetection {
    private String piiType;
    private String location;
    private String locationType;
    private String value;
    private int lineNumber;
    private String severity;
    private Date detectionTime;
    private boolean remediationRequired;

    // Getters and setters
    public String getPiiType() { return piiType; }
    public void setPiiType(String piiType) { this.piiType = piiType; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getLocationType() { return locationType; }
    public void setLocationType(String locationType) { this.locationType = locationType; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public Date getDetectionTime() { return detectionTime; }
    public void setDetectionTime(Date detectionTime) { this.detectionTime = detectionTime; }
    public boolean isRemediationRequired() { return remediationRequired; }
    public void setRemediationRequired(boolean remediationRequired) {
        this.remediationRequired = remediationRequired;
    }
}

/**
 * PII match
 */
class PIIMatch {
    private String piiType;
    private String value;
    private int startPosition;
    private int endPosition;
    private double confidence;

    // Getters and setters
    public String getPiiType() { return piiType; }
    public void setPiiType(String piiType) { this.piiType = piiType; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public int getStartPosition() { return startPosition; }
    public void setStartPosition(int startPosition) { this.startPosition = startPosition; }
    public int getEndPosition() { return endPosition; }
    public void setEndPosition(int endPosition) { this.endPosition = endPosition; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
}

/**
 * PII detection report
 */
class PIIDetectionReport {
    private Date reportDate;
    private int totalDetections;
    private Map<String, Long> detectionsByType;
    private Map<String, Long> detectionsByLocation;
    private Map<String, Long> detectionsBySeverity;
    private List<PIIDetection> criticalDetections;
    private List<String> recommendations;

    // Getters and setters
    public Date getReportDate() { return reportDate; }
    public void setReportDate(Date reportDate) { this.reportDate = reportDate; }
    public int getTotalDetections() { return totalDetections; }
    public void setTotalDetections(int totalDetections) { this.totalDetections = totalDetections; }
    public Map<String, Long> getDetectionsByType() { return detectionsByType; }
    public void setDetectionsByType(Map<String, Long> detectionsByType) {
        this.detectionsByType = detectionsByType;
    }
    public Map<String, Long> getDetectionsByLocation() { return detectionsByLocation; }
    public void setDetectionsByLocation(Map<String, Long> detectionsByLocation) {
        this.detectionsByLocation = detectionsByLocation;
    }
    public Map<String, Long> getDetectionsBySeverity() { return detectionsBySeverity; }
    public void setDetectionsBySeverity(Map<String, Long> detectionsBySeverity) {
        this.detectionsBySeverity = detectionsBySeverity;
    }
    public List<PIIDetection> getCriticalDetections() { return criticalDetections; }
    public void setCriticalDetections(List<PIIDetection> criticalDetections) {
        this.criticalDetections = criticalDetections;
    }
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }
}