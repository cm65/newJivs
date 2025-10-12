# Agent Task: jivs-test-results-analyzer

## Task Type
quality_analysis

## Phase
testing

## Context & Inputs
Analyze all test results

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
