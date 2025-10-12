package com.jivs.platform.domain.compliance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of processing a data erasure request (GDPR Article 17 - Right to be Forgotten)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErasureRequestResult {

    /**
     * Request ID
     */
    private Long requestId;

    /**
     * Data subject identifier
     */
    private String dataSubjectId;

    /**
     * Processing start time
     */
    private LocalDateTime startTime;

    /**
     * Processing end time
     */
    private LocalDateTime endTime;

    /**
     * Duration in milliseconds
     */
    private Long durationMs;

    /**
     * Success status
     */
    private Boolean success;

    /**
     * Error message if failed
     */
    private String errorMessage;

    /**
     * Total records erased
     */
    @Builder.Default
    private Integer totalErased = 0;

    /**
     * Records erased per source
     */
    @Builder.Default
    private Map<String, Integer> erasedBySource = new HashMap<>();

    /**
     * Records that could not be erased (with reasons)
     */
    @Builder.Default
    private Map<String, String> erasureFailures = new HashMap<>();

    /**
     * Sources processed
     */
    @Builder.Default
    private List<String> sourcesProcessed = new ArrayList<>();

    /**
     * Legal holds that prevented erasure
     */
    @Builder.Default
    private List<String> legalHolds = new ArrayList<>();

    /**
     * Backup data erased
     */
    private Boolean backupsErased;

    /**
     * Archive data erased
     */
    private Boolean archivesErased;

    /**
     * Processing notes
     */
    @Builder.Default
    private List<String> processingNotes = new ArrayList<>();

    /**
     * Verification timestamp
     */
    private LocalDateTime verificationTimestamp;

    /**
     * Add erased count for a source
     */
    public void addErasedRecords(String source, int count) {
        erasedBySource.put(source, erasedBySource.getOrDefault(source, 0) + count);
        totalErased += count;
    }

    /**
     * Add erasure failure
     */
    public void addErasureFailure(String source, String reason) {
        erasureFailures.put(source, reason);
    }

    /**
     * Add processed source
     */
    public void addSourceProcessed(String source) {
        if (!sourcesProcessed.contains(source)) {
            sourcesProcessed.add(source);
        }
    }

    /**
     * Add legal hold
     */
    public void addLegalHold(String holdDescription) {
        legalHolds.add(holdDescription);
    }

    /**
     * Add processing note
     */
    public void addProcessingNote(String note) {
        processingNotes.add(note);
    }

    /**
     * Convenience methods for ComplianceService compatibility
     */
    public void setDataSources(List<String> sources) {
        this.sourcesProcessed = sources;
    }

    public List<String> getDataSources() {
        return sourcesProcessed;
    }

    public void setTotalRecords(int total) {
        // In erasure context, totalRecords represents records identified for erasure
        this.totalErased = total;
    }

    public Integer getTotalRecords() {
        return totalErased;
    }

    public void setErasedRecords(int count) {
        this.totalErased = count;
    }

    public Integer getErasedRecords() {
        return totalErased;
    }

    public void setError(String error) {
        this.errorMessage = error;
        this.success = false;
    }
}
