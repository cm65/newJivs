# Agent Task: jivs-backend-architect

## Task Type
architecture

## Phase
design

## Context & Inputs
Design based on planning outputs

## Previous Agent Outputs
{
  "phase": "planning",
  "outputs": {
    "sprint_plan": "2-week sprint plan for Dark Mode UI Feature",
    "feature_priorities": ["P0: Core feature", "P1: Testing", "P2: Documentation"],
    "acceptance_criteria": ["Feature works as specified", "Tests pass", "Compliant with GDPR/CCPA"]
  }
}

## Instructions
Execute your specialized task based on the above context and inputs.
Generate structured output that will be consumed by downstream agents.

## Output Format
Provide output in JSON format with the following structure:
{
  "agent": "jivs-backend-architect",
  "task_type": "architecture",
  "status": "success|failure",
  "outputs": {
    // Agent-specific outputs
  },
  "recommendations": [],
  "next_steps": [],
  "issues": []
}
