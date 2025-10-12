package com.jivs.platform.service.archiving;

import com.jivs.platform.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Service for archiving and retrieving documents
 * Supports compression, cold storage, and long-term retention
 */
@Service
@RequiredArgsConstructor
public class DocumentArchivingService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DocumentArchivingService.class);

    private final StorageService storageService;

    /**
     * Archive a document
     */
    @Async
    @Transactional
    public CompletableFuture<ArchiveResult> archiveDocument(ArchiveRequest request) {
        log.info("Archiving document: {}", request.getDocumentId());

        ArchiveResult result = new ArchiveResult();
        result.setDocumentId(request.getDocumentId());
        result.setArchiveId(UUID.randomUUID().toString());
        result.setStartTime(new Date());

        try {
            // Get document data
            byte[] documentData = getDocumentData(request.getDocumentId());
            if (documentData == null || documentData.length == 0) {
                throw new IllegalArgumentException("Document not found or empty");
            }

            // Compress if requested
            byte[] dataToArchive = documentData;
            if (request.isCompress()) {
                dataToArchive = compress(documentData);
                result.setCompressed(true);
                result.setCompressionRatio(
                    (double) dataToArchive.length / documentData.length
                );
            }

            // Generate metadata
            ArchiveMetadata metadata = generateMetadata(request, dataToArchive);
            result.setMetadata(metadata);

            // Store in archive location
            String archivePath = storeInArchive(
                result.getArchiveId(),
                dataToArchive,
                request.getStorageTier()
            );
            result.setArchivePath(archivePath);

            // Update document status
            updateDocumentStatus(request.getDocumentId(), DocumentStatus.ARCHIVED);

            // Store archive record
            storeArchiveRecord(result, request);

            result.setSuccess(true);
            result.setEndTime(new Date());

            log.info("Document archived successfully: {} -> {}",
                request.getDocumentId(), result.getArchiveId());

        } catch (Exception e) {
            log.error("Failed to archive document: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return CompletableFuture.completedFuture(result);
    }

    /**
     * Archive multiple documents as a batch
     */
    @Async
    public CompletableFuture<BatchArchiveResult> archiveBatch(List<ArchiveRequest> requests) {
        log.info("Archiving batch of {} documents", requests.size());

        BatchArchiveResult batchResult = new BatchArchiveResult();
        batchResult.setBatchId(UUID.randomUUID().toString());
        batchResult.setTotalDocuments(requests.size());
        batchResult.setStartTime(new Date());

        List<CompletableFuture<ArchiveResult>> futures = requests.stream()
            .map(this::archiveDocument)
            .collect(Collectors.toList());

        // Wait for all to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );

        return allOf.thenApply(v -> {
            List<ArchiveResult> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

            long successful = results.stream()
                .filter(ArchiveResult::isSuccess)
                .count();

            batchResult.setSuccessfulArchives((int) successful);
            batchResult.setFailedArchives(requests.size() - (int) successful);
            batchResult.setResults(results);
            batchResult.setEndTime(new Date());

            log.info("Batch archive completed: {} successful, {} failed",
                successful, batchResult.getFailedArchives());

            return batchResult;
        });
    }

    /**
     * Retrieve archived document
     */
    public RetrievalResult retrieveArchivedDocument(String archiveId) {
        log.info("Retrieving archived document: {}", archiveId);

        RetrievalResult result = new RetrievalResult();
        result.setArchiveId(archiveId);
        result.setRetrievalTime(new Date());

        try {
            // Get archive record
            ArchiveRecord record = getArchiveRecord(archiveId);
            if (record == null) {
                throw new IllegalArgumentException("Archive not found: " + archiveId);
            }

            // Retrieve data from storage
            byte[] archivedData = retrieveFromArchive(record.getArchivePath());

            // Decompress if needed
            byte[] documentData = archivedData;
            if (record.isCompressed()) {
                documentData = decompress(archivedData);
            }

            // Verify checksum
            String checksum = calculateChecksum(documentData);
            if (!checksum.equals(record.getOriginalChecksum())) {
                log.warn("Checksum mismatch for archive: {}", archiveId);
            }

            result.setDocumentData(documentData);
            result.setOriginalDocumentId(record.getDocumentId());
            result.setSuccess(true);

            // Update access timestamp
            updateAccessTimestamp(archiveId);

            log.info("Document retrieved successfully from archive: {}", archiveId);

        } catch (Exception e) {
            log.error("Failed to retrieve archived document: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * Restore archived document to active storage
     */
    @Async
    @Transactional
    public CompletableFuture<RestoreResult> restoreDocument(String archiveId) {
        log.info("Restoring document from archive: {}", archiveId);

        RestoreResult result = new RestoreResult();
        result.setArchiveId(archiveId);
        result.setStartTime(new Date());

        try {
            // Retrieve document
            RetrievalResult retrievalResult = retrieveArchivedDocument(archiveId);
            if (!retrievalResult.isSuccess()) {
                throw new Exception("Failed to retrieve document: " +
                    retrievalResult.getErrorMessage());
            }

            // Store in active storage
            String documentId = retrievalResult.getOriginalDocumentId();
            storeInActiveStorage(documentId, retrievalResult.getDocumentData());

            // Update document status
            updateDocumentStatus(documentId, DocumentStatus.ACTIVE);

            // Optionally delete archive
            // deleteArchive(archiveId);

            result.setDocumentId(documentId);
            result.setSuccess(true);
            result.setEndTime(new Date());

            log.info("Document restored successfully: {}", archiveId);

        } catch (Exception e) {
            log.error("Failed to restore document: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return CompletableFuture.completedFuture(result);
    }

    /**
     * Delete archived document
     */
    @Transactional
    public void deleteArchive(String archiveId) {
        log.info("Deleting archive: {}", archiveId);

        try {
            ArchiveRecord record = getArchiveRecord(archiveId);
            if (record == null) {
                log.warn("Archive not found: {}", archiveId);
                return;
            }

            // Delete from storage
            deleteFromArchive(record.getArchivePath());

            // Delete archive record
            deleteArchiveRecord(archiveId);

            log.info("Archive deleted successfully: {}", archiveId);

        } catch (Exception e) {
            log.error("Failed to delete archive: {}", e.getMessage(), e);
        }
    }

    /**
     * Move archive to different storage tier
     */
    @Async
    public CompletableFuture<TierMigrationResult> migrateStorageTier(
            String archiveId,
            StorageTier newTier) {

        log.info("Migrating archive {} to tier {}", archiveId, newTier);

        TierMigrationResult result = new TierMigrationResult();
        result.setArchiveId(archiveId);
        result.setNewTier(newTier);
        result.setStartTime(new Date());

        try {
            ArchiveRecord record = getArchiveRecord(archiveId);
            if (record == null) {
                throw new IllegalArgumentException("Archive not found");
            }

            StorageTier oldTier = record.getStorageTier();
            result.setOldTier(oldTier);

            // Retrieve from old location
            byte[] data = retrieveFromArchive(record.getArchivePath());

            // Store in new tier
            String newPath = storeInArchive(archiveId, data, newTier);

            // Delete from old location
            deleteFromArchive(record.getArchivePath());

            // Update archive record
            updateArchiveTier(archiveId, newTier, newPath);

            result.setSuccess(true);
            result.setEndTime(new Date());

            log.info("Archive migrated successfully from {} to {}", oldTier, newTier);

        } catch (Exception e) {
            log.error("Failed to migrate archive tier: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return CompletableFuture.completedFuture(result);
    }

    /**
     * Get archive statistics
     */
    public ArchiveStatistics getArchiveStatistics() {
        log.debug("Getting archive statistics");

        ArchiveStatistics stats = new ArchiveStatistics();

        // TODO: Query database for statistics
        stats.setTotalArchives(0);
        stats.setTotalSizeBytes(0L);
        stats.setArchivesByTier(new HashMap<>());
        stats.setOldestArchive(null);
        stats.setNewestArchive(null);

        return stats;
    }

    /**
     * Search archives
     */
    public List<ArchiveRecord> searchArchives(ArchiveSearchCriteria criteria) {
        log.debug("Searching archives with criteria: {}", criteria);

        // TODO: Implement search functionality
        return new ArrayList<>();
    }

    /**
     * Compress data using GZIP
     */
    private byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("data");
            zos.putNextEntry(entry);
            zos.write(data);
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    /**
     * Decompress data
     */
    private byte[] decompress(byte[] compressedData) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipInputStream zis = new ZipInputStream(bais)) {
            ZipEntry entry = zis.getNextEntry();
            if (entry != null) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
            }
        }

        return baos.toByteArray();
    }

    /**
     * Store in archive storage
     */
    private String storeInArchive(String archiveId, byte[] data, StorageTier tier) {
        log.debug("Storing in archive tier: {}", tier);

        String directory = "archives/" + tier.name().toLowerCase();
        // TODO: Use StorageService to store
        return directory + "/" + archiveId;
    }

    /**
     * Retrieve from archive storage
     */
    private byte[] retrieveFromArchive(String archivePath) {
        log.debug("Retrieving from archive: {}", archivePath);

        // TODO: Use StorageService to retrieve
        return new byte[0];
    }

    /**
     * Delete from archive storage
     */
    private void deleteFromArchive(String archivePath) {
        log.debug("Deleting from archive: {}", archivePath);

        // TODO: Use StorageService to delete
    }

    /**
     * Get document data
     */
    private byte[] getDocumentData(String documentId) {
        // TODO: Retrieve document data from storage
        return new byte[0];
    }

    /**
     * Store in active storage
     */
    private void storeInActiveStorage(String documentId, byte[] data) {
        // TODO: Store document in active storage
    }

    /**
     * Update document status
     */
    private void updateDocumentStatus(String documentId, DocumentStatus status) {
        log.debug("Updating document status: {} -> {}", documentId, status);
        // TODO: Update in database
    }

    /**
     * Generate archive metadata
     */
    private ArchiveMetadata generateMetadata(ArchiveRequest request, byte[] data) {
        ArchiveMetadata metadata = new ArchiveMetadata();
        metadata.setDocumentId(request.getDocumentId());
        metadata.setArchiveDate(new Date());
        metadata.setSize(data.length);
        metadata.setChecksum(calculateChecksum(data));
        metadata.setRetentionPolicy(request.getRetentionPolicy());
        metadata.setStorageTier(request.getStorageTier());
        return metadata;
    }

    /**
     * Calculate checksum
     */
    private String calculateChecksum(byte[] data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Error calculating checksum: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Store archive record
     */
    private void storeArchiveRecord(ArchiveResult result, ArchiveRequest request) {
        log.debug("Storing archive record: {}", result.getArchiveId());
        // TODO: Store in database
    }

    /**
     * Get archive record
     */
    private ArchiveRecord getArchiveRecord(String archiveId) {
        log.debug("Getting archive record: {}", archiveId);
        // TODO: Retrieve from database
        return null;
    }

    /**
     * Delete archive record
     */
    private void deleteArchiveRecord(String archiveId) {
        log.debug("Deleting archive record: {}", archiveId);
        // TODO: Delete from database
    }

    /**
     * Update archive tier
     */
    private void updateArchiveTier(String archiveId, StorageTier tier, String newPath) {
        log.debug("Updating archive tier: {} -> {}", archiveId, tier);
        // TODO: Update in database
    }

    /**
     * Update access timestamp
     */
    private void updateAccessTimestamp(String archiveId) {
        log.debug("Updating access timestamp: {}", archiveId);
        // TODO: Update in database
    }
}

/**
 * Archive request
 */
class ArchiveRequest {
    private String documentId;
    private boolean compress;
    private StorageTier storageTier;
    private String retentionPolicy;
    private Map<String, String> metadata;

    // Getters and setters
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public boolean isCompress() { return compress; }
    public void setCompress(boolean compress) { this.compress = compress; }
    public StorageTier getStorageTier() { return storageTier; }
    public void setStorageTier(StorageTier storageTier) { this.storageTier = storageTier; }
    public String getRetentionPolicy() { return retentionPolicy; }
    public void setRetentionPolicy(String retentionPolicy) { this.retentionPolicy = retentionPolicy; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}

/**
 * Archive result
 */
class ArchiveResult {
    private String archiveId;
    private String documentId;
    private String archivePath;
    private boolean compressed;
    private double compressionRatio;
    private ArchiveMetadata metadata;
    private boolean success;
    private String errorMessage;
    private Date startTime;
    private Date endTime;

    // Getters and setters
    public String getArchiveId() { return archiveId; }
    public void setArchiveId(String archiveId) { this.archiveId = archiveId; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getArchivePath() { return archivePath; }
    public void setArchivePath(String archivePath) { this.archivePath = archivePath; }
    public boolean isCompressed() { return compressed; }
    public void setCompressed(boolean compressed) { this.compressed = compressed; }
    public double getCompressionRatio() { return compressionRatio; }
    public void setCompressionRatio(double compressionRatio) {
        this.compressionRatio = compressionRatio;
    }
    public ArchiveMetadata getMetadata() { return metadata; }
    public void setMetadata(ArchiveMetadata metadata) { this.metadata = metadata; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
}

/**
 * Batch archive result
 */
class BatchArchiveResult {
    private String batchId;
    private int totalDocuments;
    private int successfulArchives;
    private int failedArchives;
    private List<ArchiveResult> results;
    private Date startTime;
    private Date endTime;

    // Getters and setters
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public int getTotalDocuments() { return totalDocuments; }
    public void setTotalDocuments(int totalDocuments) { this.totalDocuments = totalDocuments; }
    public int getSuccessfulArchives() { return successfulArchives; }
    public void setSuccessfulArchives(int successfulArchives) {
        this.successfulArchives = successfulArchives;
    }
    public int getFailedArchives() { return failedArchives; }
    public void setFailedArchives(int failedArchives) { this.failedArchives = failedArchives; }
    public List<ArchiveResult> getResults() { return results; }
    public void setResults(List<ArchiveResult> results) { this.results = results; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
}

/**
 * Retrieval result
 */
class RetrievalResult {
    private String archiveId;
    private String originalDocumentId;
    private byte[] documentData;
    private boolean success;
    private String errorMessage;
    private Date retrievalTime;

    // Getters and setters
    public String getArchiveId() { return archiveId; }
    public void setArchiveId(String archiveId) { this.archiveId = archiveId; }
    public String getOriginalDocumentId() { return originalDocumentId; }
    public void setOriginalDocumentId(String originalDocumentId) {
        this.originalDocumentId = originalDocumentId;
    }
    public byte[] getDocumentData() { return documentData; }
    public void setDocumentData(byte[] documentData) { this.documentData = documentData; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Date getRetrievalTime() { return retrievalTime; }
    public void setRetrievalTime(Date retrievalTime) { this.retrievalTime = retrievalTime; }
}

/**
 * Restore result
 */
class RestoreResult {
    private String archiveId;
    private String documentId;
    private boolean success;
    private String errorMessage;
    private Date startTime;
    private Date endTime;

    // Getters and setters
    public String getArchiveId() { return archiveId; }
    public void setArchiveId(String archiveId) { this.archiveId = archiveId; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
}

/**
 * Tier migration result
 */
class TierMigrationResult {
    private String archiveId;
    private StorageTier oldTier;
    private StorageTier newTier;
    private boolean success;
    private String errorMessage;
    private Date startTime;
    private Date endTime;

    // Getters and setters
    public String getArchiveId() { return archiveId; }
    public void setArchiveId(String archiveId) { this.archiveId = archiveId; }
    public StorageTier getOldTier() { return oldTier; }
    public void setOldTier(StorageTier oldTier) { this.oldTier = oldTier; }
    public StorageTier getNewTier() { return newTier; }
    public void setNewTier(StorageTier newTier) { this.newTier = newTier; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
}

/**
 * Archive metadata
 */
class ArchiveMetadata {
    private String documentId;
    private Date archiveDate;
    private long size;
    private String checksum;
    private String retentionPolicy;
    private StorageTier storageTier;

    // Getters and setters
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public Date getArchiveDate() { return archiveDate; }
    public void setArchiveDate(Date archiveDate) { this.archiveDate = archiveDate; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public String getRetentionPolicy() { return retentionPolicy; }
    public void setRetentionPolicy(String retentionPolicy) { this.retentionPolicy = retentionPolicy; }
    public StorageTier getStorageTier() { return storageTier; }
    public void setStorageTier(StorageTier storageTier) { this.storageTier = storageTier; }
}

/**
 * Archive record
 */
class ArchiveRecord {
    private String archiveId;
    private String documentId;
    private String archivePath;
    private boolean compressed;
    private String originalChecksum;
    private StorageTier storageTier;
    private Date archiveDate;
    private Date lastAccessDate;

    // Getters and setters
    public String getArchiveId() { return archiveId; }
    public void setArchiveId(String archiveId) { this.archiveId = archiveId; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getArchivePath() { return archivePath; }
    public void setArchivePath(String archivePath) { this.archivePath = archivePath; }
    public boolean isCompressed() { return compressed; }
    public void setCompressed(boolean compressed) { this.compressed = compressed; }
    public String getOriginalChecksum() { return originalChecksum; }
    public void setOriginalChecksum(String originalChecksum) {
        this.originalChecksum = originalChecksum;
    }
    public StorageTier getStorageTier() { return storageTier; }
    public void setStorageTier(StorageTier storageTier) { this.storageTier = storageTier; }
    public Date getArchiveDate() { return archiveDate; }
    public void setArchiveDate(Date archiveDate) { this.archiveDate = archiveDate; }
    public Date getLastAccessDate() { return lastAccessDate; }
    public void setLastAccessDate(Date lastAccessDate) { this.lastAccessDate = lastAccessDate; }
}

/**
 * Archive statistics
 */
class ArchiveStatistics {
    private int totalArchives;
    private long totalSizeBytes;
    private Map<StorageTier, Integer> archivesByTier;
    private Date oldestArchive;
    private Date newestArchive;

    // Getters and setters
    public int getTotalArchives() { return totalArchives; }
    public void setTotalArchives(int totalArchives) { this.totalArchives = totalArchives; }
    public long getTotalSizeBytes() { return totalSizeBytes; }
    public void setTotalSizeBytes(long totalSizeBytes) { this.totalSizeBytes = totalSizeBytes; }
    public Map<StorageTier, Integer> getArchivesByTier() { return archivesByTier; }
    public void setArchivesByTier(Map<StorageTier, Integer> archivesByTier) {
        this.archivesByTier = archivesByTier;
    }
    public Date getOldestArchive() { return oldestArchive; }
    public void setOldestArchive(Date oldestArchive) { this.oldestArchive = oldestArchive; }
    public Date getNewestArchive() { return newestArchive; }
    public void setNewestArchive(Date newestArchive) { this.newestArchive = newestArchive; }
}

/**
 * Archive search criteria
 */
class ArchiveSearchCriteria {
    private String documentId;
    private StorageTier storageTier;
    private Date archiveDateFrom;
    private Date archiveDateTo;
    private String retentionPolicy;

    // Getters and setters
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public StorageTier getStorageTier() { return storageTier; }
    public void setStorageTier(StorageTier storageTier) { this.storageTier = storageTier; }
    public Date getArchiveDateFrom() { return archiveDateFrom; }
    public void setArchiveDateFrom(Date archiveDateFrom) { this.archiveDateFrom = archiveDateFrom; }
    public Date getArchiveDateTo() { return archiveDateTo; }
    public void setArchiveDateTo(Date archiveDateTo) { this.archiveDateTo = archiveDateTo; }
    public String getRetentionPolicy() { return retentionPolicy; }
    public void setRetentionPolicy(String retentionPolicy) { this.retentionPolicy = retentionPolicy; }
}

/**
 * Storage tiers
 */
enum StorageTier {
    HOT,         // Frequently accessed, fast retrieval
    WARM,        // Occasionally accessed
    COLD,        // Rarely accessed, slower retrieval
    GLACIER      // Long-term archival, very slow retrieval
}

/**
 * Document status
 */
enum DocumentStatus {
    ACTIVE,
    ARCHIVED,
    DELETED
}
