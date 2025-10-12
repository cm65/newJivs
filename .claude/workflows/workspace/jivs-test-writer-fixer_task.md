# Agent Task: jivs-test-writer-fixer

## Task Type
test_creation

## Phase
testing

## Context & Inputs
Create comprehensive tests

## Previous Agent Outputs
{
  "phase": "infrastructure",
  "outputs": {
    "kubernetes_manifests": "Deployment, Service, HPA, ConfigMap",
    "ci_cd_pipeline": "GitHub Actions with build, test, deploy stages",
    "monitoring_config": "Prometheus metrics + Grafana dashboards",
    "backup_jobs": "CronJob for daily PostgreSQL backups"
  }
}

## Instructions
Execute your specialized task based on the above context and inputs.
Generate structured output that will be consumed by downstream agents.

## Output Format
Provide output in JSON format with the following structure:
{
  "agent": "jivs-test-writer-fixer",
  "task_type": "test_creation",
  "status": "success|failure",
  "outputs": {
    // Agent-specific outputs
  },
  "recommendations": [],
  "next_steps": [],
  "issues": []
}
