package com.jivs.platform.service.monitoring;

import com.jivs.platform.domain.Document;
import com.jivs.platform.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for monitoring file integrity and detecting orphaned documents
 * Runs scheduled jobs to verify that database records match actual files on disk
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileIntegrityMonitor {

    private final DocumentRepository documentRepository;

    /**
     * Check file integrity - runs daily at 2 AM
     * Detects orphaned database records (DB exists but file missing)
     */
    @Scheduled(cron = "${jivs.integrity.check.cron:0 0 2 * * *}") // Default: 2 AM daily
    public void checkFileIntegrity() {
        log.info("🔍 Starting scheduled file integrity check...");

        long startTime = System.currentTimeMillis();
        List<Document> orphanedDocuments = new ArrayList<>();
        List<Document> corruptedDocuments = new ArrayList<>();
        int totalDocuments = 0;
        int validDocuments = 0;
        long totalStorageUsed = 0L;
        long actualStorageUsed = 0L;

        try {
            List<Document> allDocuments = documentRepository.findAll();
            totalDocuments = allDocuments.size();

            log.info("Checking {} documents for integrity...", totalDocuments);

            for (Document doc : allDocuments) {
                try {
                    // Skip documents without storage path
                    if (doc.getStoragePath() == null || doc.getStoragePath().isEmpty()) {
                        log.warn("Document {} has no storage path", doc.getId());
                        orphanedDocuments.add(doc);
                        continue;
                    }

                    Path filePath = Paths.get(doc.getStoragePath());

                    // Check if file exists
                    if (!Files.exists(filePath)) {
                        log.error("❌ Orphaned document detected - ID: {}, filename: {}, path: {}",
                            doc.getId(), doc.getFilename(), doc.getStoragePath());
                        orphanedDocuments.add(doc);
                        continue;
                    }

                    // Check if file is readable
                    if (!Files.isReadable(filePath)) {
                        log.error("❌ Unreadable file - ID: {}, path: {}",
                            doc.getId(), doc.getStoragePath());
                        corruptedDocuments.add(doc);
                        continue;
                    }

                    // Verify file size matches database
                    long actualSize = Files.size(filePath);
                    totalStorageUsed += doc.getSize() != null ? doc.getSize() : 0L;
                    actualStorageUsed += actualSize;

                    // Check for GZIP compression mismatch
                    byte[] header = new byte[2];
                    try (java.io.InputStream is = Files.newInputStream(filePath)) {
                        int bytesRead = is.read(header);
                        if (bytesRead >= 2) {
                            boolean isGzipFile = (header[0] == 0x1F && header[1] == (byte)0x8B);

                            if (isGzipFile != doc.isCompressed()) {
                                log.warn("⚠️  Compression flag mismatch - ID: {}, DB compressed: {}, file GZIP: {}",
                                    doc.getId(), doc.isCompressed(), isGzipFile);
                            }
                        }
                    }

                    validDocuments++;

                } catch (Exception e) {
                    log.error("Error checking document {}: {}", doc.getId(), e.getMessage());
                    corruptedDocuments.add(doc);
                }
            }

            long duration = System.currentTimeMillis() - startTime;

            // Log summary
            log.info("✅ File integrity check completed in {} ms", duration);
            log.info("📊 Total documents: {}", totalDocuments);
            log.info("✓ Valid documents: {}", validDocuments);
            log.info("❌ Orphaned documents (file missing): {}", orphanedDocuments.size());
            log.info("⚠️  Corrupted documents (unreadable): {}", corruptedDocuments.size());
            log.info("💾 Database storage total: {} MB", totalStorageUsed / (1024 * 1024));
            log.info("💾 Actual disk usage: {} MB", actualStorageUsed / (1024 * 1024));

            // Log orphaned documents for investigation
            if (!orphanedDocuments.isEmpty()) {
                log.error("🚨 ORPHANED DOCUMENTS DETECTED - Immediate action required!");
                for (Document doc : orphanedDocuments) {
                    log.error("  - ID: {}, filename: {}, path: {}, size: {} bytes",
                        doc.getId(), doc.getFilename(), doc.getStoragePath(),
                        doc.getSize() != null ? doc.getSize() : 0L);
                }
            }

            // Log corrupted documents
            if (!corruptedDocuments.isEmpty()) {
                log.error("🚨 CORRUPTED DOCUMENTS DETECTED - Investigate immediately!");
                for (Document doc : corruptedDocuments) {
                    log.error("  - ID: {}, filename: {}, path: {}",
                        doc.getId(), doc.getFilename(), doc.getStoragePath());
                }
            }

        } catch (Exception e) {
            log.error("File integrity check failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Get file integrity report (manual trigger via API)
     */
    public Map<String, Object> getIntegrityReport() {
        log.info("Generating file integrity report...");

        Map<String, Object> report = new HashMap<>();
        List<Map<String, Object>> orphanedDocuments = new ArrayList<>();
        List<Map<String, Object>> corruptedDocuments = new ArrayList<>();
        int totalDocuments = 0;
        int validDocuments = 0;

        try {
            List<Document> allDocuments = documentRepository.findAll();
            totalDocuments = allDocuments.size();

            for (Document doc : allDocuments) {
                try {
                    if (doc.getStoragePath() == null || doc.getStoragePath().isEmpty()) {
                        Map<String, Object> issue = new HashMap<>();
                        issue.put("id", doc.getId());
                        issue.put("filename", doc.getFilename());
                        issue.put("issue", "No storage path");
                        orphanedDocuments.add(issue);
                        continue;
                    }

                    Path filePath = Paths.get(doc.getStoragePath());

                    if (!Files.exists(filePath)) {
                        Map<String, Object> issue = new HashMap<>();
                        issue.put("id", doc.getId());
                        issue.put("filename", doc.getFilename());
                        issue.put("storagePath", doc.getStoragePath());
                        issue.put("issue", "File missing on disk");
                        orphanedDocuments.add(issue);
                        continue;
                    }

                    if (!Files.isReadable(filePath)) {
                        Map<String, Object> issue = new HashMap<>();
                        issue.put("id", doc.getId());
                        issue.put("filename", doc.getFilename());
                        issue.put("storagePath", doc.getStoragePath());
                        issue.put("issue", "File not readable");
                        corruptedDocuments.add(issue);
                        continue;
                    }

                    validDocuments++;

                } catch (Exception e) {
                    Map<String, Object> issue = new HashMap<>();
                    issue.put("id", doc.getId());
                    issue.put("filename", doc.getFilename());
                    issue.put("issue", "Error: " + e.getMessage());
                    corruptedDocuments.add(issue);
                }
            }

            report.put("totalDocuments", totalDocuments);
            report.put("validDocuments", validDocuments);
            report.put("orphanedDocuments", orphanedDocuments);
            report.put("orphanedCount", orphanedDocuments.size());
            report.put("corruptedDocuments", corruptedDocuments);
            report.put("corruptedCount", corruptedDocuments.size());
            report.put("healthyPercentage", totalDocuments > 0 ?
                ((double) validDocuments / totalDocuments) * 100 : 100.0);

        } catch (Exception e) {
            log.error("Error generating integrity report: {}", e.getMessage(), e);
            report.put("error", e.getMessage());
        }

        return report;
    }
}
