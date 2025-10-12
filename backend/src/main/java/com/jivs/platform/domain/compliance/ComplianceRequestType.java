package com.jivs.platform.domain.compliance;

/**
 * Enum representing compliance request types
 */
public enum ComplianceRequestType {
    /**
     * GDPR Article 15 - Right of Access
     */
    ACCESS,

    /**
     * GDPR Article 17 - Right to Erasure (Right to be Forgotten)
     */
    ERASURE,

    /**
     * GDPR Article 16 - Right to Rectification
     */
    RECTIFICATION,

    /**
     * GDPR Article 20 - Right to Data Portability
     */
    PORTABILITY,

    /**
     * GDPR Article 21 - Right to Object
     */
    OBJECTION,

    /**
     * GDPR Article 18 - Right to Restriction of Processing
     */
    RESTRICTION,

    /**
     * Data breach notification
     */
    DATA_BREACH,

    /**
     * Consent withdrawal
     */
    CONSENT_WITHDRAWAL,

    /**
     * Other compliance request
     */
    OTHER
}
