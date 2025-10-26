package com.jivs.platform.service.workflow;

/**
 * Functional interface for executing workflow steps
 */
@FunctionalInterface
public interface StepExecutor {
    StepResult execute(WorkflowExecution execution) throws Exception;
}
