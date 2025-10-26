package com.jivs.platform.service.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a workflow step execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepResult {
    private boolean success;
    private String stepId;
    private String errorMessage;
    private Object result;
}
