package com.jivs.platform.service.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a workflow with steps and execution policies
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinition {
    private String workflowId;
    private String workflowName;
    
    @Builder.Default
    private List<WorkflowStep> steps = new ArrayList<>();
    
    private boolean rollbackEnabled;
    private long timeoutMs;
}
