# Agent Task: jivs-project-shipper

## Task Type
release_management

## Phase
release

## Context & Inputs
Prepare and execute release

## Previous Agent Outputs
{
  "phase": "operations",
  "outputs": {
    "prometheus_alerts": "20 alerts configured",
    "grafana_dashboards": "JiVS Feature Dashboard created",
    "analytics_dashboards": "Feature usage analytics ready",
    "health_checks": "All systems healthy",
    "velocity_metrics": "Sprint velocity: 45 story points"
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
