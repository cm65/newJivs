# Agent Task: jivs-workflow-optimizer

## Task Type
process_optimization

## Phase
operations

## Context & Inputs
Optimize workflows

## Previous Agent Outputs
{
  "phase": "compliance",
  "outputs": {
    "compliance_report": "GDPR: COMPLIANT, CCPA: COMPLIANT",
    "audit_trail": "Complete audit logging implemented",
    "security_issues": "0 CRITICAL, 0 HIGH, 2 MEDIUM, 5 LOW",
    "compliance_approval": "APPROVED"
  }
}

## Instructions
Execute your specialized task based on the above context and inputs.
Generate structured output that will be consumed by downstream agents.

## Output Format
Provide output in JSON format with the following structure:
{
  "agent": "jivs-workflow-optimizer",
  "task_type": "process_optimization",
  "status": "success|failure",
  "outputs": {
    // Agent-specific outputs
  },
  "recommendations": [],
  "next_steps": [],
  "issues": []
}
