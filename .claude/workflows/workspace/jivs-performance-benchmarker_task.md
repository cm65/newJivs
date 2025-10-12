# Agent Task: jivs-performance-benchmarker

## Task Type
performance_testing

## Phase
testing

## Context & Inputs
Benchmark performance

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
  "agent": "jivs-performance-benchmarker",
  "task_type": "performance_testing",
  "status": "success|failure",
  "outputs": {
    // Agent-specific outputs
  },
  "recommendations": [],
  "next_steps": [],
  "issues": []
}
