package com.jivs.platform.service.workflow;

/**
 * Workflow execution status
 */
public enum WorkflowStatus {
    PENDING,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED,
    ROLLED_BACK
}
