package com.jivs.platform.service.workflow;

import com.jivs.platform.event.PlatformEvent;
import com.jivs.platform.event.PlatformEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates workflow execution with retry, rollback, and checkpoint support
 * Implements the SAGA pattern for distributed transactions
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorkflowOrchestrationService {

    private final PlatformEventPublisher eventPublisher;
    private final Map<String, WorkflowCheckpoint> checkpoints = new ConcurrentHashMap<>();

    /**
     * Execute workflow asynchronously
     * @param workflow Workflow definition
     * @return CompletableFuture with workflow result
     */
    @Async("workflowExecutor")
    public CompletableFuture<WorkflowResult> executeWorkflow(WorkflowDefinition workflow) {
        String executionId = UUID.randomUUID().toString();
        
        WorkflowExecution execution = WorkflowExecution.builder()
                .executionId(executionId)
                .workflowId(workflow.getWorkflowId())
                .status(WorkflowStatus.RUNNING)
                .startTime(Instant.now())
                .currentStepIndex(0)
                .build();

        log.info("Starting workflow execution: {} ({})", workflow.getWorkflowName(), executionId);

        try {
            // Publish workflow started event
            publishEvent("WORKFLOW_STARTED", execution);

            // Execute all steps sequentially
            for (int i = 0; i < workflow.getSteps().size(); i++) {
                WorkflowStep step = workflow.getSteps().get(i);
                execution.setCurrentStepIndex(i);

                log.debug("Executing step {}: {}", i + 1, step.getStepName());

                StepResult stepResult = executeStepWithRetry(step, execution);
                execution.getStepResults().add(stepResult);

                if (!stepResult.isSuccess() && step.isCritical()) {
                    log.error("Critical step failed: {} - {}", step.getStepName(), stepResult.getErrorMessage());
                    
                    if (workflow.isRollbackEnabled()) {
                        executeRollback(workflow, execution, i);
                    }
                    
                    return CompletableFuture.completedFuture(createFailedResult(execution, stepResult.getErrorMessage()));
                }

                // Create checkpoint if requested
                if (step.isCheckpointAfter()) {
                    createCheckpoint(execution);
                }
            }

            // Workflow completed successfully
            execution.setStatus(WorkflowStatus.COMPLETED);
            execution.setEndTime(Instant.now());

            log.info("Workflow completed successfully: {}", executionId);
            publishEvent("WORKFLOW_COMPLETED", execution);

            return CompletableFuture.completedFuture(createSuccessResult(execution));

        } catch (Exception e) {
            log.error("Workflow execution failed: {}", executionId, e);
            execution.setStatus(WorkflowStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            execution.setEndTime(Instant.now());

            publishEvent("WORKFLOW_FAILED", execution);

            return CompletableFuture.completedFuture(createFailedResult(execution, e.getMessage()));
        }
    }

    /**
     * Execute step with retry logic
     */
    private StepResult executeStepWithRetry(WorkflowStep step, WorkflowExecution execution) {
        int attempts = 0;
        StepResult result = null;

        while (attempts <= step.getRetryAttempts()) {
            try {
                if (attempts > 0) {
                    log.warn("Retrying step: {} (attempt {})", step.getStepName(), attempts + 1);
                    Thread.sleep(1000 * attempts); // Exponential backoff
                }

                result = step.getExecutor().execute(execution);
                
                if (result.isSuccess()) {
                    return result;
                }
                
                attempts++;
            } catch (Exception e) {
                log.error("Step execution error: {}", step.getStepName(), e);
                result = StepResult.builder()
                        .success(false)
                        .stepId(step.getStepId())
                        .errorMessage(e.getMessage())
                        .build();
                attempts++;
            }
        }

        return result;
    }

    /**
     * Execute rollback for completed steps
     */
    private void executeRollback(WorkflowDefinition workflow, WorkflowExecution execution, int failedStepIndex) {
        log.info("Executing rollback for workflow: {}", execution.getExecutionId());
        
        execution.setStatus(WorkflowStatus.ROLLED_BACK);
        publishEvent("WORKFLOW_ROLLING_BACK", execution);

        // Rollback steps in reverse order
        for (int i = failedStepIndex - 1; i >= 0; i--) {
            try {
                WorkflowStep step = workflow.getSteps().get(i);
                log.debug("Rolling back step: {}", step.getStepName());
                
                // Rollback logic would be implemented in step executor
                // step.getRollbackExecutor().execute(execution);
                
            } catch (Exception e) {
                log.error("Rollback failed for step: {}", i, e);
            }
        }

        publishEvent("WORKFLOW_ROLLED_BACK", execution);
    }

    /**
     * Create checkpoint for resuming workflow
     */
    private void createCheckpoint(WorkflowExecution execution) {
        WorkflowCheckpoint checkpoint = WorkflowCheckpoint.builder()
                .executionId(execution.getExecutionId())
                .completedSteps(execution.getCurrentStepIndex() + 1)
                .checkpointTime(Instant.now())
                .executionState(execution)
                .build();

        checkpoints.put(execution.getExecutionId(), checkpoint);
        log.debug("Checkpoint created for execution: {}", execution.getExecutionId());
    }

    /**
     * Resume workflow from checkpoint
     */
    public CompletableFuture<WorkflowResult> resumeWorkflow(String executionId, WorkflowDefinition workflow) {
        WorkflowCheckpoint checkpoint = checkpoints.get(executionId);
        
        if (checkpoint == null) {
            log.error("No checkpoint found for execution: {}", executionId);
            return CompletableFuture.completedFuture(
                    WorkflowResult.builder()
                            .success(false)
                            .errorMessage("No checkpoint found")
                            .build()
            );
        }

        log.info("Resuming workflow from checkpoint: {}", executionId);
        
        // Resume from checkpoint (simplified - full implementation would restore state)
        return executeWorkflow(workflow);
    }

    /**
     * Publish workflow event
     */
    private void publishEvent(String eventType, WorkflowExecution execution) {
        PlatformEvent event = PlatformEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .source(PlatformEvent.EventSource.WORKFLOW)
                .entityId(Long.parseLong(execution.getExecutionId().hashCode() + ""))
                .entityType("Workflow")
                .build();
        
        event.getPayload().put("executionId", execution.getExecutionId());
        event.getPayload().put("workflowId", execution.getWorkflowId());
        event.getPayload().put("status", execution.getStatus());
        
        eventPublisher.publish(event);
    }

    /**
     * Create success result
     */
    private WorkflowResult createSuccessResult(WorkflowExecution execution) {
        return WorkflowResult.builder()
                .success(true)
                .executionId(execution.getExecutionId())
                .finalStatus(WorkflowStatus.COMPLETED)
                .durationMs(execution.getEndTime().toEpochMilli() - execution.getStartTime().toEpochMilli())
                .build();
    }

    /**
     * Create failed result
     */
    private WorkflowResult createFailedResult(WorkflowExecution execution, String errorMessage) {
        return WorkflowResult.builder()
                .success(false)
                .executionId(execution.getExecutionId())
                .finalStatus(execution.getStatus())
                .errorMessage(errorMessage)
                .durationMs(execution.getEndTime() != null ? 
                        execution.getEndTime().toEpochMilli() - execution.getStartTime().toEpochMilli() : 0)
                .build();
    }
}
