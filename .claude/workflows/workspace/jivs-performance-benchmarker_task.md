# Agent Task: jivs-performance-benchmarker

## Task Type
performance_testing

## Phase
testing

## Context & Inputs
Benchmark performance

## Previous Agent Outputs
{
  "phase": "testing",
  "outputs": {
    "test_coverage": "85%",
    "unit_tests_passing": "100%",
    "integration_tests_passing": "98%",
    "api_test_results": "All endpoints returning 200 OK",
    "performance_metrics": "p95 latency: 120ms, p99: 280ms",
    "quality_score": "92/100",
    "go_no_go_decision": "GO - All quality gates passed"
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
