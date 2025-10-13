package com.jivs.platform.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Result DTO for document archiving operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveResult {
    private boolean success;
    private String archiveId;
    private List<String> archivedDocumentIds;
    private List<String> failedDocumentIds;
    private Map<String, String> errors;
    private String archivePath;
    private String archiveType; // HOT, WARM, COLD, GLACIER
    private Long originalSize;
    private Long compressedSize;
    private Double compressionRatio;
    private Date archiveDate;
    private Date retentionDate;
    private String encryptionAlgorithm;
    private String checksum;
    private Integer totalDocuments;
    private Integer successfullyArchived;
    private Integer failedToArchive;
    private Long processingTimeMillis;
    private String message;

    public static ArchiveResult success(String archiveId, List<String> documentIds) {
        ArchiveResult result = new ArchiveResult();
        result.setSuccess(true);
        result.setArchiveId(archiveId);
        result.setArchivedDocumentIds(documentIds);
        result.setSuccessfullyArchived(documentIds.size());
        result.setTotalDocuments(documentIds.size());
        result.setFailedToArchive(0);
        result.setArchiveDate(new Date());
        return result;
    }

    public static ArchiveResult failure(String message) {
        ArchiveResult result = new ArchiveResult();
        result.setSuccess(false);
        result.setMessage(message);
        result.setArchiveDate(new Date());
        return result;
    }
}