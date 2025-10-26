package com.jivs.platform.service.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single step in a workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStep {
    private String stepId;
    private String stepName;
    private StepExecutor executor;
    private boolean critical;
    private boolean checkpointAfter;
    private int retryAttempts;
    private long timeoutMs;
}
