# Agent Task: jivs-test-writer-fixer

## Task Type
test_creation

## Phase
testing

## Context & Inputs
Create comprehensive tests

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
