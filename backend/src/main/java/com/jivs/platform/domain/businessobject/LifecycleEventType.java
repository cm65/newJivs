package com.jivs.platform.domain.businessobject;

/**
 * Enum representing types of lifecycle events for business objects
 */
public enum LifecycleEventType {
    /**
     * Business object was created
     */
    CREATED,

    /**
     * Business object was updated
     */
    UPDATED,

    /**
     * Business object was archived
     */
    ARCHIVED,

    /**
     * Business object was deleted
     */
    DELETED,

    /**
     * Business object was activated
     */
    ACTIVATED,

    /**
     * Business object was deactivated
     */
    DEACTIVATED,

    /**
     * Business object was approved
     */
    APPROVED,

    /**
     * Business object was rejected
     */
    REJECTED,

    /**
     * Business object was published
     */
    PUBLISHED,

    /**
     * Business object was unpublished
     */
    UNPUBLISHED,

    /**
     * Business object was locked
     */
    LOCKED,

    /**
     * Business object was unlocked
     */
    UNLOCKED,

    /**
     * Business object version was created
     */
    VERSIONED,

    /**
     * Business object was restored
     */
    RESTORED
}
