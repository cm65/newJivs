package com.jivs.platform.service.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Checkpoint for resuming workflow execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowCheckpoint {
    private String executionId;
    private int completedSteps;
    private Instant checkpointTime;
    private WorkflowExecution executionState;
}
