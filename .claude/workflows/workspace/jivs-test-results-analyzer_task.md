# Agent Task: jivs-test-results-analyzer

## Task Type
quality_analysis

## Phase
testing

## Context & Inputs
Analyze all test results

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
  "agent": "jivs-test-results-analyzer",
  "task_type": "quality_analysis",
  "status": "success|failure",
  "outputs": {
    // Agent-specific outputs
  },
  "recommendations": [],
  "next_steps": [],
  "issues": []
}
