package com.jivs.platform.service.archiving;

import com.jivs.platform.domain.Document;
import com.jivs.platform.dto.DocumentDTO;
import com.jivs.platform.repository.DocumentRepository;
import com.jivs.platform.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Helper class for compressing and decompressing document files
 * Implements atomic file operations to prevent corruption
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentCompressionHelper {

    private final DocumentService documentService;
    private final DocumentRepository documentRepository;

    /**
     * Compress a document file in-place with atomic writes
     *
     * @param documentId ID of the document to compress
     * @param storageTier Storage tier for the archived document (HOT, WARM, COLD)
     * @return Map with compression results (success, compressionRatio, originalSize, compressedSize)
     * @throws IOException if compression fails
     */
    public Map<String, Object> compressDocumentFile(Long documentId, String storageTier) throws IOException {
        log.info("Starting compression for document {}", documentId);

        // 1. Get document entity from database (need storagePath which is not in DTO)
        Optional<Document> docOpt = documentRepository.findById(documentId);
        if (!docOpt.isPresent()) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        Document doc = docOpt.get();

        // 2. Check if already compressed (prevent double compression)
        if (doc.isCompressed()) {
            log.info("Document {} already compressed. Skipping.", documentId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("alreadyCompressed", true);
            result.put("compressionRatio", doc.getCompressionRatio());
            result.put("message", "Document already compressed");
            return result;
        }

        // 3. Validate storage path
        if (doc.getStoragePath() == null || doc.getStoragePath().isEmpty()) {
            throw new IllegalStateException("Document " + documentId + " has no storage path");
        }

        Path originalPath = Paths.get(doc.getStoragePath());

        // 4. Validate file exists
        if (!Files.exists(originalPath)) {
            throw new FileNotFoundException("File not found: " + doc.getStoragePath());
        }

        // 5. Validate file is readable
        if (!Files.isReadable(originalPath)) {
            throw new IOException("File not readable: " + doc.getStoragePath());
        }

        // 6. Validate file is not empty
        long fileSize = Files.size(originalPath);
        if (fileSize == 0) {
            throw new IOException("File is empty: " + doc.getStoragePath());
        }

        // 7. Check file header to detect if already compressed
        byte[] header = new byte[2];
        try (InputStream is = Files.newInputStream(originalPath)) {
            int bytesRead = is.read(header);
            if (bytesRead >= 2 && header[0] == 0x1F && header[1] == (byte)0x8B) {
                // GZIP magic bytes detected
                log.warn("Document {} not marked as compressed in DB, but file is GZIP. Fixing DB...", documentId);
                doc.setCompressed(true);
                doc.setArchived(true);
                doc.setStorageTier(storageTier);
                documentRepository.save(doc);

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("alreadyCompressed", true);
                result.put("fixedDatabase", true);
                result.put("message", "File already compressed, updated database");
                return result;
            }
        }

        // 8. Perform compression with atomic writes
        Path tempPath = null;
        Path backupPath = null;

        try {
            // Create temp and backup paths
            tempPath = originalPath.resolveSibling(originalPath.getFileName() + ".tmp");
            backupPath = originalPath.resolveSibling(originalPath.getFileName() + ".backup");

            // Read original file
            byte[] fileData = Files.readAllBytes(originalPath);
            long originalSize = fileData.length;

            // Compress data using GZIP
            byte[] compressed = compressData(fileData);
            long compressedSize = compressed.length;

            // Calculate compression ratio
            double compressionRatio = (double) compressedSize / originalSize;

            log.info("Document {} compression: {} bytes → {} bytes (ratio: {:.2f})",
                documentId, originalSize, compressedSize, compressionRatio);

            // Check if compression actually helped (skip if ratio >= 0.95)
            if (compressionRatio >= 0.95) {
                log.info("Document {} compression ratio {:.2f} too poor. Keeping original but marking as archived.",
                    documentId, compressionRatio);

                // Don't replace file, but mark as archived (not compressed)
                doc.setArchived(true);
                doc.setStorageTier(storageTier);
                doc.setCompressed(false);
                doc.setCompressionRatio(compressionRatio);
                documentRepository.save(doc);

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("compressed", false);
                result.put("archived", true);
                result.put("compressionRatio", compressionRatio);
                result.put("originalSize", originalSize);
                result.put("message", "File doesn't compress well, kept original");
                return result;
            }

            // Write compressed data to temp file
            Files.write(tempPath, compressed);

            // Verify temp file was written correctly
            if (!Files.exists(tempPath) || Files.size(tempPath) == 0) {
                throw new IOException("Compressed temp file is empty or missing");
            }

            // Create backup of original (safety net)
            Files.copy(originalPath, backupPath, StandardCopyOption.REPLACE_EXISTING);

            // ATOMIC REPLACE: Move temp file to original location
            Files.move(tempPath, originalPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            // Update database (only after file successfully replaced)
            doc.setCompressed(true);
            doc.setCompressionRatio(compressionRatio);
            doc.setArchived(true);
            doc.setStorageTier(storageTier);
            documentRepository.save(doc);

            // Delete backup (cleanup)
            Files.deleteIfExists(backupPath);

            log.info("Successfully compressed and archived document {}", documentId);

            // Return success result
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("compressed", true);
            result.put("archived", true);
            result.put("compressionRatio", compressionRatio);
            result.put("originalSize", originalSize);
            result.put("compressedSize", compressedSize);
            result.put("spaceSaved", originalSize - compressedSize);
            result.put("message", "Document compressed successfully");
            return result;

        } catch (Exception e) {
            // Rollback: Restore from backup if original was corrupted
            if (backupPath != null && Files.exists(backupPath)) {
                try {
                    Files.copy(backupPath, originalPath, StandardCopyOption.REPLACE_EXISTING);
                    Files.deleteIfExists(backupPath);
                    log.info("Restored original file from backup after compression failure");
                } catch (IOException restoreEx) {
                    log.error("Failed to restore backup: {}", restoreEx.getMessage());
                }
            }

            // Cleanup temp file
            if (tempPath != null) {
                try {
                    Files.deleteIfExists(tempPath);
                } catch (IOException cleanupEx) {
                    log.error("Failed to cleanup temp file: {}", cleanupEx.getMessage());
                }
            }

            log.error("Failed to compress document {}: {}", documentId, e.getMessage(), e);
            throw new IOException("Failed to compress document: " + e.getMessage(), e);
        }
    }

    /**
     * Compress data using GZIP
     */
    private byte[] compressData(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(data);
            gzipOut.finish();
        }
        return baos.toByteArray();
    }

    /**
     * Decompress GZIP data
     */
    private byte[] decompressData(byte[] compressedData) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (GZIPInputStream gzipIn = new GZIPInputStream(bais)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = gzipIn.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
        }

        return baos.toByteArray();
    }
}
