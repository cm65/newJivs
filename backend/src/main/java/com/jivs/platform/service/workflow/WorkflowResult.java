package com.jivs.platform.service.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a complete workflow execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResult {
    private boolean success;
    private String executionId;
    private WorkflowStatus finalStatus;
    private String errorMessage;
    private long durationMs;
}
