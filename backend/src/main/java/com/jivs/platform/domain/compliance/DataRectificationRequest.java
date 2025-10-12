package com.jivs.platform.domain.compliance;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Request for data rectification (GDPR Article 16)
 */
@NoArgsConstructor
@AllArgsConstructor
public class DataRectificationRequest {

    /**
     * Data subject identifier (email, user ID, etc.)
     */
    private String dataSubjectId;

    /**
     * Type of data subject identifier
     */
    private String dataSubjectIdType;

    /**
     * Reason for rectification
     */
    private String reason;

    /**
     * Description of inaccurate data
     */
    private String inaccurateDataDescription;

    /**
     * Fields to be corrected
     * Map of field name to new value
     */
    private Map<String, Object> corrections = new HashMap<>();

    /**
     * Specific sources to update
     */
    private java.util.List<String> targetSources;

    /**
     * Whether to notify third parties
     */
    private Boolean notifyThirdParties = false;

    /**
     * Additional notes
     */
    private String notes;

    /**
     * User ID making the request
     */
    private Long userId;

    /**
     * Contact email for updates
     */
    private String contactEmail;

    /**
     * Contact phone for updates
     */
    private String contactPhone;

    /**
     * Add a correction
     */
    public void addCorrection(String fieldName, Object newValue) {
        corrections.put(fieldName, newValue);
    }

    /**
     * Convenience method for ComplianceService compatibility
     */
    public String getEmail() {
        return contactEmail;
    }

    /**
     * Convenience method for ComplianceService compatibility
     */
    public void setEmail(String email) {
        this.contactEmail = email;
    }

    /**
     * Convenience method for ComplianceService compatibility
     */
    public String getIdentifier() {
        return dataSubjectId;
    }

    /**
     * Convenience method for ComplianceService compatibility
     */
    public void setIdentifier(String identifier) {
        this.dataSubjectId = identifier;
    }

    // Manual getters and setters
    public String getDataSubjectId() {
        return dataSubjectId;
    }

    public void setDataSubjectId(String dataSubjectId) {
        this.dataSubjectId = dataSubjectId;
    }

    public String getDataSubjectIdType() {
        return dataSubjectIdType;
    }

    public void setDataSubjectIdType(String dataSubjectIdType) {
        this.dataSubjectIdType = dataSubjectIdType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getInaccurateDataDescription() {
        return inaccurateDataDescription;
    }

    public void setInaccurateDataDescription(String inaccurateDataDescription) {
        this.inaccurateDataDescription = inaccurateDataDescription;
    }

    public Map<String, Object> getCorrections() {
        return corrections;
    }

    public void setCorrections(Map<String, Object> corrections) {
        this.corrections = corrections;
    }

    public java.util.List<String> getTargetSources() {
        return targetSources;
    }

    public void setTargetSources(java.util.List<String> targetSources) {
        this.targetSources = targetSources;
    }

    public Boolean getNotifyThirdParties() {
        return notifyThirdParties;
    }

    public void setNotifyThirdParties(Boolean notifyThirdParties) {
        this.notifyThirdParties = notifyThirdParties;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
}
