# Agent Task: jivs-devops-automator

## Task Type
infrastructure

## Phase
infrastructure

## Context & Inputs
Setup based on design

## Previous Agent Outputs
{
  "phase": "design",
  "outputs": {
    "service_design": "Spring Boot service with controllers, services, repositories",
    "api_endpoints": ["/api/v1/feature", "/api/v1/feature/{id}"],
    "database_schema": "feature_table with columns: id, name, data, created_at",
    "component_design": "React components: FeatureList, FeatureDetail, FeatureForm",
    "redux_slices": "featureSlice with CRUD operations"
  }
}

## Instructions
Execute your specialized task based on the above context and inputs.
Generate structured output that will be consumed by downstream agents.

## Output Format
Provide output in JSON format with the following structure:
{
  "agent": "jivs-devops-automator",
  "task_type": "infrastructure",
  "status": "success|failure",
  "outputs": {
    // Agent-specific outputs
  },
  "recommendations": [],
  "next_steps": [],
  "issues": []
}
