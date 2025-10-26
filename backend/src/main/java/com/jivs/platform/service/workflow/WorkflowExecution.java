package com.jivs.platform.service.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a running workflow execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecution {
    private String executionId;
    private String workflowId;
    private WorkflowStatus status;
    private Instant startTime;
    private Instant endTime;
    
    @Builder.Default
    private List<StepResult> stepResults = new ArrayList<>();
    
    @Builder.Default
    private Map<String, Object> context = new HashMap<>();
    
    private int currentStepIndex;
    private String errorMessage;
}
