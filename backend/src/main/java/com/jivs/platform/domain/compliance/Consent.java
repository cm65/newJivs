package com.jivs.platform.domain.compliance;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing user consent for data processing
 * Implements GDPR Article 7 - Conditions for consent
 */
@Entity
@Table(name = "consents", indexes = {
    @Index(name = "idx_consent_user", columnList = "user_id"),
    @Index(name = "idx_consent_purpose", columnList = "purpose"),
    @Index(name = "idx_consent_status", columnList = "status")
})
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Consent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String subjectEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsentPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsentStatus status = ConsentStatus.PENDING;

    @Column
    private Boolean consentGiven = false;

    @Column
    private LocalDateTime consentDate;

    @Column
    private String legalBasis;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String consentText;

    @Column(nullable = false)
    private String consentVersion;

    @Column
    private LocalDateTime grantedAt;

    @Column
    private LocalDateTime withdrawnAt;

    @Column
    private LocalDateTime withdrawnDate;

    @Column
    private LocalDateTime expiresAt;

    @Column
    private String withdrawalReason;

    @Column(nullable = false)
    private String consentMethod;  // WEB_FORM, API, EMAIL, MOBILE_APP, etc.

    @Column
    private String ipAddress;

    @Column
    private String userAgent;

    @Column
    private String geolocation;

    @ElementCollection
    @CollectionTable(name = "consent_metadata", joinColumns = @JoinColumn(name = "consent_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value", columnDefinition = "TEXT")
    private Map<String, String> metadata = new HashMap<>();

    @Column
    private Boolean isExplicit = true;  // Explicit vs implicit consent

    @Column
    private Boolean isInformed = true;  // Was user properly informed

    @Column
    private Boolean isFreelyGiven = true;  // Was consent freely given

    @Column
    private Boolean isSpecific = true;  // Was consent specific to purpose

    @Column
    private Boolean isUnambiguous = true;  // Was consent unambiguous

    @Column
    private Boolean active = false;

    @Column(columnDefinition = "TEXT")
    private String proofOfConsent;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime lastModifiedDate;

    /**
     * Helper method to check if consent is currently valid
     */
    @Transient
    public boolean isValid() {
        if (status != ConsentStatus.GRANTED) {
            return false;
        }
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }
        return true;
    }

    /**
     * Helper method to withdraw consent
     */
    public void withdraw(String reason) {
        this.status = ConsentStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now();
        this.withdrawalReason = reason;
    }

    /**
     * Helper method to check if consent is expiring soon (within 30 days)
     */
    @Transient
    public boolean isExpiringSoon() {
        if (expiresAt == null) {
            return false;
        }
        LocalDateTime thirtyDaysFromNow = LocalDateTime.now().plusDays(30);
        return expiresAt.isBefore(thirtyDaysFromNow) && status == ConsentStatus.GRANTED;
    }

    /**
     * Convenience method for ComplianceService compatibility
     */
    public boolean isConsentGiven() {
        return consentGiven != null && consentGiven;
    }

    /**
     * Convenience method for ComplianceService compatibility
     */
    public boolean isActive() {
        return active != null && active;
    }

    /**
     * Convenience method for ComplianceService compatibility
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Convenience method for ComplianceService compatibility
     */
    public void setConsentGiven(boolean consentGiven) {
        this.consentGiven = consentGiven;
    }

    /**
     * Convenience method - alias for expiresAt
     */
    public LocalDateTime getExpiryDate() {
        return expiresAt;
    }

    /**
     * Convenience method - alias for expiresAt
     */
    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiresAt = expiryDate;
    }

    // Manual getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public ConsentStatus getStatus() {
        return status;
    }

    public void setStatus(ConsentStatus status) {
        this.status = status;
    }

    public Boolean getConsentGiven() {
        return consentGiven;
    }

    public LocalDateTime getConsentDate() {
        return consentDate;
    }

    public void setConsentDate(LocalDateTime consentDate) {
        this.consentDate = consentDate;
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

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    public LocalDateTime getWithdrawnAt() {
        return withdrawnAt;
    }

    public void setWithdrawnAt(LocalDateTime withdrawnAt) {
        this.withdrawnAt = withdrawnAt;
    }

    public LocalDateTime getWithdrawnDate() {
        return withdrawnDate;
    }

    public void setWithdrawnDate(LocalDateTime withdrawnDate) {
        this.withdrawnDate = withdrawnDate;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getWithdrawalReason() {
        return withdrawalReason;
    }

    public void setWithdrawalReason(String withdrawalReason) {
        this.withdrawalReason = withdrawalReason;
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

    public Boolean getActive() {
        return active;
    }

    public String getProofOfConsent() {
        return proofOfConsent;
    }

    public void setProofOfConsent(String proofOfConsent) {
        this.proofOfConsent = proofOfConsent;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
