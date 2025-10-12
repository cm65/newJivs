# Agent Task: jivs-frontend-developer

## Task Type
ui_design

## Phase
design

## Context & Inputs
Design based on backend API

## Previous Agent Outputs
{
  "phase": "planning",
  "outputs": {
    "sprint_plan": "2-week sprint plan for Advanced Filtering and Sorting - Dynamic filters with saved views",
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
  "agent": "jivs-frontend-developer",
  "task_type": "ui_design",
  "status": "success|failure",
  "outputs": {
    // Agent-specific outputs
  },
  "recommendations": [],
  "next_steps": [],
  "issues": []
}
