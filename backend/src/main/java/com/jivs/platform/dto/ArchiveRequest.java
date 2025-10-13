package com.jivs.platform.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * Request DTO for document archiving operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveRequest {
    // Single document fields
    private String documentId;
    private String storageTier; // HOT, WARM, COLD, GLACIER
    private boolean deleteOriginal = false;

    // Batch operation fields
    private List<String> documentIds;
    private String archiveReason;
    private String archiveType; // HOT, WARM, COLD, GLACIER
    private boolean compress = true;
    private boolean encrypt = true;
    private Date retentionDate;
    private String archiveNotes;
    private String requestedBy;

    // For batch archiving
    private boolean batchMode = false;
    private Integer batchSize = 100;
    private boolean validateBeforeArchive = true;

    // Archive policy
    private String policyId;
    private boolean applyRetentionPolicy = true;
}