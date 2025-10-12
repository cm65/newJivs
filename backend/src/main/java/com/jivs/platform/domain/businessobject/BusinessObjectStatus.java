package com.jivs.platform.domain.businessobject;

/**
 * Enum representing business object status
 */
public enum BusinessObjectStatus {
    /**
     * Object is in draft state
     */
    DRAFT,

    /**
     * Object is active and in use
     */
    ACTIVE,

    /**
     * Object is inactive but not deleted
     */
    INACTIVE,

    /**
     * Object is pending approval
     */
    PENDING,

    /**
     * Object has been approved
     */
    APPROVED,

    /**
     * Object has been rejected
     */
    REJECTED,

    /**
     * Object is archived
     */
    ARCHIVED,

    /**
     * Object is marked for deletion
     */
    DELETED,

    /**
     * Object is locked for editing
     */
    LOCKED,

    /**
     * Object is under review
     */
    UNDER_REVIEW
}
