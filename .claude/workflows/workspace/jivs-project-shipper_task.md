# Agent Task: jivs-project-shipper

## Task Type
release_management

## Phase
release

## Context & Inputs
Prepare and execute release

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
  "agent": "jivs-project-shipper",
  "task_type": "release_management",
  "status": "success|failure",
  "outputs": {
    // Agent-specific outputs
  },
  "recommendations": [],
  "next_steps": [],
  "issues": []
}
