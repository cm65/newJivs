# Agent Task: jivs-sprint-prioritizer

## Task Type
prioritization

## Phase
planning

## Context & Inputs
Feature request: Zero-Trust Security Model - Implement zero-trust architecture with mutual TLS and service mesh

## Previous Agent Outputs
{
  "phase": "release",
  "outputs": {
    "release_plan": "Release v1.2.3 to production",
    "deployment_checklist": "All items completed",
    "rollback_plan": "Automated rollback configured",
    "customer_communication": "Release notes sent to customers",
    "release_completed": true
  }
}

## Instructions
Execute your specialized task based on the above context and inputs.
Generate structured output that will be consumed by downstream agents.

## Output Format
Provide output in JSON format with the following structure:
{
  "agent": "jivs-sprint-prioritizer",
  "task_type": "prioritization",
  "status": "success|failure",
  "outputs": {
    // Agent-specific outputs
  },
  "recommendations": [],
  "next_steps": [],
  "issues": []
}
