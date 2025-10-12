#!/usr/bin/env node
/**
 * JiVS Platform - Load Test Results Comparison Tool
 *
 * This Node.js script compares baseline vs. optimized performance results
 * and generates a comprehensive markdown report.
 *
 * Usage:
 *   node compare-results.js
 *   node compare-results.js --baseline baseline.json --optimized performance.json
 *   node compare-results.js --output comparison-report.md
 */

const fs = require('fs');
const path = require('path');

// Configuration
const RESULTS_DIR = './load-test-results';
const DEFAULT_BASELINE = `${RESULTS_DIR}/baseline-latest.json`;
const DEFAULT_OPTIMIZED = `${RESULTS_DIR}/performance-latest.json`;
const DEFAULT_OUTPUT = `${RESULTS_DIR}/comparison-report.md`;

// Performance targets from optimization plan
const TARGETS = {
    throughput: 20000,            // records/min
    latency_p95: 200,             // ms
    latency_p99: 500,             // ms
    error_rate: 1,                // percent
    throughput_improvement: 100,  // percent (2x = 100% improvement)
    latency_reduction: 56,        // percent
};

// ANSI colors
const colors = {
    reset: '\x1b[0m',
    green: '\x1b[32m',
    yellow: '\x1b[33m',
    red: '\x1b[31m',
    blue: '\x1b[34m',
    cyan: '\x1b[36m',
};

/**
 * Parse command line arguments
 */
function parseArgs() {
    const args = process.argv.slice(2);
    const config = {
        baseline: DEFAULT_BASELINE,
        optimized: DEFAULT_OPTIMIZED,
        output: DEFAULT_OUTPUT,
    };

    for (let i = 0; i < args.length; i++) {
        if (args[i] === '--baseline' && args[i + 1]) {
            config.baseline = args[i + 1];
            i++;
        } else if (args[i] === '--optimized' && args[i + 1]) {
            config.optimized = args[i + 1];
            i++;
        } else if (args[i] === '--output' && args[i + 1]) {
            config.output = args[i + 1];
            i++;
        } else if (args[i] === '--help') {
            console.log(`
Usage: node compare-results.js [options]

Options:
  --baseline <file>   Path to baseline results JSON (default: ${DEFAULT_BASELINE})
  --optimized <file>  Path to optimized results JSON (default: ${DEFAULT_OPTIMIZED})
  --output <file>     Path to output markdown report (default: ${DEFAULT_OUTPUT})
  --help              Show this help message

Examples:
  node compare-results.js
  node compare-results.js --baseline baseline.json --optimized performance.json
  node compare-results.js --output my-report.md
            `);
            process.exit(0);
        }
    }

    return config;
}

/**
 * Load JSON file
 */
function loadJSON(filePath) {
    try {
        const content = fs.readFileSync(filePath, 'utf8');
        return JSON.parse(content);
    } catch (error) {
        console.error(`${colors.red}Error loading ${filePath}:${colors.reset}`, error.message);
        process.exit(1);
    }
}

/**
 * Calculate percentage change
 */
function calculateChange(oldValue, newValue) {
    if (oldValue === 0) return 'N/A';
    return ((newValue - oldValue) / oldValue * 100).toFixed(2);
}

/**
 * Calculate percentage reduction
 */
function calculateReduction(oldValue, newValue) {
    if (oldValue === 0) return 'N/A';
    return ((oldValue - newValue) / oldValue * 100).toFixed(2);
}

/**
 * Format number with commas
 */
function formatNumber(num) {
    if (typeof num !== 'number') return num;
    return num.toLocaleString('en-US', { maximumFractionDigits: 2 });
}

/**
 * Generate comparison report
 */
function generateReport(baseline, optimized) {
    const timestamp = new Date().toISOString();

    // Extract metrics
    const baselineMetrics = {
        throughput: baseline.metrics?.extraction?.records_per_minute ||
                   baseline.metrics?.extraction?.avg_records_per_extraction || 0,
        latency_p95: baseline.metrics?.extraction?.creation_latency_p95 ||
                     baseline.metrics?.http?.p95_duration_ms || 0,
        latency_p99: baseline.metrics?.extraction?.latency_p99 ||
                     baseline.metrics?.http?.p99_duration_ms || 0,
        error_rate: baseline.metrics?.extraction?.error_rate ||
                   baseline.metrics?.http?.failure_rate || 0,
        created: baseline.metrics?.extraction?.created || 0,
        completed: baseline.metrics?.extraction?.completed || 0,
    };

    const optimizedMetrics = {
        throughput: optimized.metrics?.extraction?.records_per_minute ||
                   optimized.metrics?.extraction?.avg_records_per_extraction || 0,
        latency_p95: optimized.metrics?.extraction?.creation_latency_p95 ||
                     optimized.metrics?.http?.p95_duration_ms || 0,
        latency_p99: optimized.metrics?.extraction?.latency_p99 ||
                     optimized.metrics?.http?.p99_duration_ms || 0,
        error_rate: optimized.metrics?.extraction?.error_rate ||
                   optimized.metrics?.http?.failure_rate || 0,
        created: optimized.metrics?.extraction?.created || 0,
        completed: optimized.metrics?.extraction?.completed || 0,
    };

    // Calculate improvements
    const throughputImprovement = calculateChange(baselineMetrics.throughput, optimizedMetrics.throughput);
    const latencyReduction = calculateReduction(baselineMetrics.latency_p95, optimizedMetrics.latency_p95);
    const errorRateChange = calculateChange(baselineMetrics.error_rate, optimizedMetrics.error_rate);

    // Check target achievement
    const targetsAchieved = {
        throughput: optimizedMetrics.throughput >= TARGETS.throughput,
        latency_p95: optimizedMetrics.latency_p95 <= TARGETS.latency_p95,
        latency_p99: optimizedMetrics.latency_p99 <= TARGETS.latency_p99,
        error_rate: parseFloat(optimizedMetrics.error_rate) <= TARGETS.error_rate,
        throughput_improvement: parseFloat(throughputImprovement) >= TARGETS.throughput_improvement,
        latency_reduction: parseFloat(latencyReduction) >= TARGETS.latency_reduction,
    };

    const allTargetsMet = Object.values(targetsAchieved).every(v => v === true);

    // Generate markdown report
    let report = `# JiVS Platform - Extraction Performance Comparison Report

**Generated:** ${new Date(timestamp).toLocaleString()}

## Executive Summary

`;

    if (allTargetsMet) {
        report += `✅ **ALL PERFORMANCE TARGETS ACHIEVED**\n\n`;
    } else {
        report += `⚠️  **Some performance targets not met**\n\n`;
    }

    report += `### Key Improvements

- **Throughput:** ${formatNumber(baselineMetrics.throughput)} → ${formatNumber(optimizedMetrics.throughput)} records/min (**${throughputImprovement}%** improvement)
- **Latency (p95):** ${formatNumber(baselineMetrics.latency_p95)}ms → ${formatNumber(optimizedMetrics.latency_p95)}ms (**${latencyReduction}%** reduction)
- **Latency (p99):** ${formatNumber(baselineMetrics.latency_p99)}ms → ${formatNumber(optimizedMetrics.latency_p99)}ms
- **Error Rate:** ${baselineMetrics.error_rate}% → ${optimizedMetrics.error_rate}%

---

## Detailed Comparison

### Throughput

| Metric | Baseline | Optimized | Change |
|--------|----------|-----------|--------|
| Records/Min | ${formatNumber(baselineMetrics.throughput)} | ${formatNumber(optimizedMetrics.throughput)} | **+${throughputImprovement}%** |
| Extractions Created | ${formatNumber(baselineMetrics.created)} | ${formatNumber(optimizedMetrics.created)} | ${calculateChange(baselineMetrics.created, optimizedMetrics.created)}% |
| Extractions Completed | ${formatNumber(baselineMetrics.completed)} | ${formatNumber(optimizedMetrics.completed)} | ${calculateChange(baselineMetrics.completed, optimizedMetrics.completed)}% |

${targetsAchieved.throughput ? '✅' : '❌'} **Target:** ≥ ${formatNumber(TARGETS.throughput)} records/min
${targetsAchieved.throughput_improvement ? '✅' : '❌'} **Improvement Target:** ≥ ${TARGETS.throughput_improvement}% improvement

### Latency

| Metric | Baseline | Optimized | Change |
|--------|----------|-----------|--------|
| p95 Latency | ${formatNumber(baselineMetrics.latency_p95)}ms | ${formatNumber(optimizedMetrics.latency_p95)}ms | **-${latencyReduction}%** |
| p99 Latency | ${formatNumber(baselineMetrics.latency_p99)}ms | ${formatNumber(optimizedMetrics.latency_p99)}ms | -${calculateReduction(baselineMetrics.latency_p99, optimizedMetrics.latency_p99)}% |

${targetsAchieved.latency_p95 ? '✅' : '❌'} **p95 Target:** ≤ ${TARGETS.latency_p95}ms
${targetsAchieved.latency_p99 ? '✅' : '❌'} **p99 Target:** ≤ ${TARGETS.latency_p99}ms
${targetsAchieved.latency_reduction ? '✅' : '❌'} **Reduction Target:** ≥ ${TARGETS.latency_reduction}% reduction

### Reliability

| Metric | Baseline | Optimized | Change |
|--------|----------|-----------|--------|
| Error Rate | ${baselineMetrics.error_rate}% | ${optimizedMetrics.error_rate}% | ${errorRateChange}% |

${targetsAchieved.error_rate ? '✅' : '❌'} **Error Rate Target:** < ${TARGETS.error_rate}%

---

## Target Achievement Summary

| Target | Status | Details |
|--------|--------|---------|
| Throughput ≥ ${formatNumber(TARGETS.throughput)} records/min | ${targetsAchieved.throughput ? '✅ PASS' : '❌ FAIL'} | Achieved: ${formatNumber(optimizedMetrics.throughput)} records/min |
| Throughput Improvement ≥ ${TARGETS.throughput_improvement}% | ${targetsAchieved.throughput_improvement ? '✅ PASS' : '❌ FAIL'} | Achieved: ${throughputImprovement}% |
| Latency p95 ≤ ${TARGETS.latency_p95}ms | ${targetsAchieved.latency_p95 ? '✅ PASS' : '❌ FAIL'} | Achieved: ${formatNumber(optimizedMetrics.latency_p95)}ms |
| Latency p99 ≤ ${TARGETS.latency_p99}ms | ${targetsAchieved.latency_p99 ? '✅ PASS' : '❌ FAIL'} | Achieved: ${formatNumber(optimizedMetrics.latency_p99)}ms |
| Latency Reduction ≥ ${TARGETS.latency_reduction}% | ${targetsAchieved.latency_reduction ? '✅ PASS' : '❌ FAIL'} | Achieved: ${latencyReduction}% |
| Error Rate < ${TARGETS.error_rate}% | ${targetsAchieved.error_rate ? '✅ PASS' : '❌ FAIL'} | Achieved: ${optimizedMetrics.error_rate}% |

---

## Optimization Impact Analysis

### P0 Optimizations Implemented

Based on the performance plan, the following P0 optimizations were implemented:

1. **P0.1: Batch Processing with Parallel Streams**
   - Batch size: 1,000 records
   - Parallel threads: 4
   - Expected impact: +40% throughput, -100ms latency

2. **P0.2: Connection Pooling for Extraction Sources**
   - HikariCP pools per data source
   - Pool size: 10 connections
   - Expected impact: +25% throughput, -80ms latency

3. **P0.3: Redis Caching for DataSource Configurations**
   - Cache TTL: 1 hour (dataSources), 30 min (configs)
   - Expected impact: +10% throughput, -50ms latency

4. **P0.4: Query Optimization with JOIN FETCH**
   - Eliminated N+1 queries
   - Expected impact: +5% throughput, -20ms latency

### Expected vs Actual Results

| Optimization | Expected Throughput | Expected Latency | Notes |
|--------------|---------------------|------------------|-------|
| P0 Combined | 20,200 records/min | 200ms p95 | Target from plan |
| **Actual** | **${formatNumber(optimizedMetrics.throughput)} records/min** | **${formatNumber(optimizedMetrics.latency_p95)}ms p95** | ${allTargetsMet ? '✅ Met targets' : '⚠️ Review needed'} |

---

## Recommendations

`;

    if (allTargetsMet) {
        report += `### ✅ Optimization Successful

All performance targets have been achieved! The P0 optimizations delivered:
- ${throughputImprovement}% throughput improvement (target: ${TARGETS.throughput_improvement}%)
- ${latencyReduction}% latency reduction (target: ${TARGETS.latency_reduction}%)

**Next Steps:**
1. Monitor production deployment closely
2. Validate with real-world data patterns
3. Consider implementing P1 optimizations for additional 10% improvement:
   - Bulk database updates
   - Database indexes
   - Connection pool tuning

`;
    } else {
        report += `### ⚠️ Optimization Review Needed

Some targets were not fully achieved. Analysis:\n\n`;

        if (!targetsAchieved.throughput) {
            report += `- **Throughput:** Achieved ${formatNumber(optimizedMetrics.throughput)} records/min (target: ${formatNumber(TARGETS.throughput)})\n`;
            report += `  → Review connection pool sizes and parallel thread configuration\n\n`;
        }

        if (!targetsAchieved.latency_p95) {
            report += `- **Latency p95:** Achieved ${formatNumber(optimizedMetrics.latency_p95)}ms (target: ${TARGETS.latency_p95}ms)\n`;
            report += `  → Check cache hit rates and database query performance\n\n`;
        }

        if (!targetsAchieved.throughput_improvement) {
            report += `- **Throughput Improvement:** Achieved ${throughputImprovement}% (target: ${TARGETS.throughput_improvement}%)\n`;
            report += `  → May need P1 optimizations to reach 2x target\n\n`;
        }

        report += `**Recommended Actions:**
1. Review application logs for bottlenecks
2. Analyze cache hit rates (target: 70%+)
3. Monitor connection pool utilization
4. Consider implementing P1 optimizations earlier
5. Profile specific slow operations

`;
    }

    report += `---

## Test Configuration

### Baseline Test
- **Type:** ${baseline.test_type || 'N/A'}
- **Duration:** ${baseline.duration_seconds || 'N/A'}s
- **Timestamp:** ${baseline.timestamp || 'N/A'}

### Optimized Test
- **Type:** ${optimized.test_type || 'N/A'}
- **Duration:** ${optimized.duration_seconds || 'N/A'}s
- **Timestamp:** ${optimized.timestamp || 'N/A'}

---

## Appendix

### Full Baseline Results
\`\`\`json
${JSON.stringify(baseline.metrics, null, 2)}
\`\`\`

### Full Optimized Results
\`\`\`json
${JSON.stringify(optimized.metrics, null, 2)}
\`\`\`

---

*Report generated by JiVS Platform Load Test Comparison Tool*
`;

    return report;
}

/**
 * Main function
 */
function main() {
    console.log(`${colors.blue}========================================`);
    console.log('JiVS Load Test Results Comparison');
    console.log(`========================================${colors.reset}\n`);

    const config = parseArgs();

    console.log(`${colors.cyan}Loading baseline results:${colors.reset} ${config.baseline}`);
    const baseline = loadJSON(config.baseline);

    console.log(`${colors.cyan}Loading optimized results:${colors.reset} ${config.optimized}`);
    const optimized = loadJSON(config.optimized);

    console.log(`${colors.cyan}Generating comparison report...${colors.reset}`);
    const report = generateReport(baseline, optimized);

    console.log(`${colors.cyan}Writing report to:${colors.reset} ${config.output}\n`);
    fs.writeFileSync(config.output, report);

    console.log(`${colors.green}✓ Report generated successfully!${colors.reset}\n`);
    console.log(`View report: cat ${config.output}`);
    console.log(`Or open in browser with a markdown viewer.\n`);
}

// Run main function
main();
