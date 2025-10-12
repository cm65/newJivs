package com.jivs.platform.domain.compliance;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO representing a consent request
 * This is not an entity, just a data transfer object
 */
@NoArgsConstructor
@AllArgsConstructor
public class ConsentRequest {

    private Long userId;

    private String subjectEmail;

    private ConsentPurpose purpose;

    private Boolean consentGiven = true;

    private String legalBasis;

    private String consentText;

    private String consentVersion;

    private LocalDateTime expiresAt;

    private String consentMethod;

    private String ipAddress;

    private String userAgent;

    private String geolocation;

    private Map<String, String> metadata = new HashMap<>();

    private Boolean isExplicit = true;

    private Boolean isInformed = true;

    private Boolean isFreelyGiven = true;

    private Boolean isSpecific = true;

    private Boolean isUnambiguous = true;

    private String proofOfConsent;

    /**
     * Convenience method for ComplianceService compatibility
     */
    public boolean isConsentGiven() {
        return consentGiven != null && consentGiven;
    }

    /**
     * Get expiry date - alias for expiresAt
     */
    public LocalDateTime getExpiryDate() {
        return expiresAt;
    }

    /**
     * Set expiry date - alias for expiresAt
     */
    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiresAt = expiryDate;
    }

    // Manual getters and setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSubjectEmail() {
        return subjectEmail;
    }

    public void setSubjectEmail(String subjectEmail) {
        this.subjectEmail = subjectEmail;
    }

    public ConsentPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(ConsentPurpose purpose) {
        this.purpose = purpose;
    }

    public Boolean getConsentGiven() {
        return consentGiven;
    }

    public void setConsentGiven(Boolean consentGiven) {
        this.consentGiven = consentGiven;
    }

    public String getLegalBasis() {
        return legalBasis;
    }

    public void setLegalBasis(String legalBasis) {
        this.legalBasis = legalBasis;
    }

    public String getConsentText() {
        return consentText;
    }

    public void setConsentText(String consentText) {
        this.consentText = consentText;
    }

    public String getConsentVersion() {
        return consentVersion;
    }

    public void setConsentVersion(String consentVersion) {
        this.consentVersion = consentVersion;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getConsentMethod() {
        return consentMethod;
    }

    public void setConsentMethod(String consentMethod) {
        this.consentMethod = consentMethod;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getGeolocation() {
        return geolocation;
    }

    public void setGeolocation(String geolocation) {
        this.geolocation = geolocation;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public Boolean getIsExplicit() {
        return isExplicit;
    }

    public void setIsExplicit(Boolean isExplicit) {
        this.isExplicit = isExplicit;
    }

    public Boolean getIsInformed() {
        return isInformed;
    }

    public void setIsInformed(Boolean isInformed) {
        this.isInformed = isInformed;
    }

    public Boolean getIsFreelyGiven() {
        return isFreelyGiven;
    }

    public void setIsFreelyGiven(Boolean isFreelyGiven) {
        this.isFreelyGiven = isFreelyGiven;
    }

    public Boolean getIsSpecific() {
        return isSpecific;
    }

    public void setIsSpecific(Boolean isSpecific) {
        this.isSpecific = isSpecific;
    }

    public Boolean getIsUnambiguous() {
        return isUnambiguous;
    }

    public void setIsUnambiguous(Boolean isUnambiguous) {
        this.isUnambiguous = isUnambiguous;
    }

    public String getProofOfConsent() {
        return proofOfConsent;
    }

    public void setProofOfConsent(String proofOfConsent) {
        this.proofOfConsent = proofOfConsent;
    }
}
