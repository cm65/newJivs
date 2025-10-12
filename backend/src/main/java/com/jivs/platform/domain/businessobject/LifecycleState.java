package com.jivs.platform.domain.businessobject;

/**
 * Enum representing lifecycle states of a business object
 */
public enum LifecycleState {
    /**
     * Object is in draft state
     */
    DRAFT,

    /**
     * Object is pending review
     */
    PENDING_REVIEW,

    /**
     * Object is approved
     */
    APPROVED,

    /**
     * Object is active
     */
    ACTIVE,

    /**
     * Object is inactive
     */
    INACTIVE,

    /**
     * Object is archived
     */
    ARCHIVED,

    /**
     * Object is deprecated
     */
    DEPRECATED,

    /**
     * Object is deleted
     */
    DELETED
}
