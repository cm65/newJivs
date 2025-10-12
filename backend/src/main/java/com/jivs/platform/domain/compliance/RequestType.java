package com.jivs.platform.domain.compliance;

/**
 * Enum representing types of compliance requests
 */
public enum RequestType {
    ACCESS,         // GDPR Article 15 - Right of access
    ERASURE,        // GDPR Article 17 - Right to erasure (right to be forgotten)
    RECTIFICATION,  // GDPR Article 16 - Right to rectification
    PORTABILITY,    // GDPR Article 20 - Right to data portability
    RESTRICTION,    // GDPR Article 18 - Right to restriction of processing
    OBJECTION,      // GDPR Article 21 - Right to object
    CONSENT_WITHDRAWAL,  // Withdraw consent
    DO_NOT_SELL     // CCPA - Do not sell my personal information
}
