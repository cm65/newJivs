package com.jivs.platform.domain.compliance;

/**
 * Enum representing consent status
 */
public enum ConsentStatus {
    GRANTED,        // Consent has been given
    WITHDRAWN,      // Consent has been withdrawn
    EXPIRED,        // Consent has expired
    PENDING         // Consent is pending (awaiting user action)
}
