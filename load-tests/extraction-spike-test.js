/**
 * JiVS Platform - Extraction Spike Test
 *
 * This test simulates sudden traffic spikes to validate:
 * - System recovery after sudden load increases
 * - Auto-scaling behavior (if configured)
 * - Circuit breaker effectiveness
 * - Connection pool elasticity
 * - Cache behavior under sudden load
 *
 * Load Profile:
 * - Normal load: 20 VUs
 * - Sudden spike: 200 VUs (10x increase)
 * - Multiple spike cycles
 *
 * Usage:
 *   k6 run load-tests/extraction-spike-test.js
 *   k6 run --env SPIKE_MULTIPLIER=15 load-tests/extraction-spike-test.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend, Counter, Gauge } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

// Custom metrics for spike analysis
const extractionLatency = new Trend('extraction_latency');
const extractionLatencyDuringSpike = new Trend('extraction_latency_during_spike');
const extractionLatencyAfterSpike = new Trend('extraction_latency_after_spike');
const extractionErrors = new Rate('extraction_errors');
const errorsDuringSpike = new Counter('errors_during_spike');
const errorsAfterSpike = new Counter('errors_after_spike');
const extractionsCreated = new Counter('extractions_created');
const currentLoad = new Gauge('current_load_vus');
const spikePhase = new Gauge('spike_phase'); // 0=normal, 1=spike, 2=recovery

// Spike test configuration
const NORMAL_LOAD = __ENV.NORMAL_LOAD || 20;
const SPIKE_MULTIPLIER = __ENV.SPIKE_MULTIPLIER || 10;
const SPIKE_LOAD = NORMAL_LOAD * SPIKE_MULTIPLIER;

export const options = {
    stages: [
        // Warmup
        { duration: '2m', target: parseInt(NORMAL_LOAD) },

        // Spike Cycle 1
        { duration: '10s', target: parseInt(NORMAL_LOAD) },    // Normal
        { duration: '30s', target: parseInt(SPIKE_LOAD) },     // Sudden spike
        { duration: '2m', target: parseInt(NORMAL_LOAD) },     // Recovery

        // Spike Cycle 2
        { duration: '1m', target: parseInt(NORMAL_LOAD) },     // Normal
        { duration: '30s', target: parseInt(SPIKE_LOAD) },     // Sudden spike
        { duration: '2m', target: parseInt(NORMAL_LOAD) },     // Recovery

        // Spike Cycle 3 - Bigger spike
        { duration: '1m', target: parseInt(NORMAL_LOAD) },     // Normal
        { duration: '1m', target: parseInt(SPIKE_LOAD * 1.5) }, // Bigger spike
        { duration: '3m', target: parseInt(NORMAL_LOAD) },     // Extended recovery

        // Cooldown
        { duration: '1m', target: 0 },
    ],
    thresholds: {
        'http_req_duration': ['p(95)<1000'],
        'http_req_failed': ['rate<0.05'],
        'extraction_errors': ['rate<0.05'],
        'extraction_latency_during_spike': ['p(95)<2000'], // Allow higher latency during spike
        'extraction_latency_after_spike': ['p(95)<500'],   // Should recover quickly
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';
const USERNAME = __ENV.TEST_USERNAME || 'testuser';
const PASSWORD = __ENV.TEST_PASSWORD || 'TestPassword123!';

// Track test phase
let testPhase = 'warmup';
let phaseStartTime = Date.now();

/**
 * Setup function
 */
export function setup() {
    console.log('==================================================');
    console.log('JiVS Extraction Spike Test');
    console.log('==================================================');
    console.log(`Target: ${BASE_URL}`);
    console.log(`Normal Load: ${NORMAL_LOAD} VUs`);
    console.log(`Spike Load: ${SPIKE_LOAD} VUs (${SPIKE_MULTIPLIER}x)`);
    console.log('');
    console.log('Spike Cycles:');
    console.log('  1. Normal → Spike → Recovery');
    console.log('  2. Normal → Spike → Recovery');
    console.log('  3. Normal → BIG Spike → Recovery');
    console.log('');

    const loginRes = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
        username: USERNAME,
        password: PASSWORD,
    }), {
        headers: { 'Content-Type': 'application/json' },
    });

    if (loginRes.status !== 200) {
        throw new Error('Authentication failed during setup');
    }

    const authToken = loginRes.json('accessToken') || loginRes.json('data.accessToken');

    return {
        baseUrl: BASE_URL,
        authToken: authToken,
        testStartTime: Date.now(),
    };
}

/**
 * Determine current test phase based on elapsed time
 */
function getCurrentPhase(elapsedSeconds) {
    // Warmup: 0-120s
    if (elapsedSeconds < 120) return 'warmup';

    // Cycle 1: 120-310s
    if (elapsedSeconds < 130) return 'normal';
    if (elapsedSeconds < 160) return 'spike';
    if (elapsedSeconds < 280) return 'recovery';

    // Cycle 2: 280-430s
    if (elapsedSeconds < 340) return 'normal';
    if (elapsedSeconds < 370) return 'spike';
    if (elapsedSeconds < 490) return 'recovery';

    // Cycle 3: 490-670s
    if (elapsedSeconds < 550) return 'normal';
    if (elapsedSeconds < 610) return 'spike';
    if (elapsedSeconds < 790) return 'recovery';

    return 'cooldown';
}

/**
 * Main spike test scenario
 */
export default function (data) {
    const headers = {
        'Authorization': `Bearer ${data.authToken}`,
        'Content-Type': 'application/json',
    };

    const elapsedSeconds = (Date.now() - data.testStartTime) / 1000;
    const currentPhase = getCurrentPhase(elapsedSeconds);

    // Update phase gauge
    const phaseValue = currentPhase === 'spike' ? 1 : (currentPhase === 'recovery' ? 2 : 0);
    spikePhase.add(phaseValue);
    currentLoad.add(__VU);

    let extractionId = null;

    // 1. Create extraction
    group('Create Extraction', function () {
        const extractionConfig = {
            name: `Spike Test ${Date.now()}-${__VU}`,
            sourceType: 'JDBC',
            connectionConfig: {
                jdbcUrl: 'jdbc:postgresql://postgres:5432/testdb',
                username: 'testuser',
                password: 'testpass',
                driverClass: 'org.postgresql.Driver',
            },
            extractionQuery: 'SELECT * FROM test_table LIMIT 5000',
        };

        const startTime = Date.now();
        const createRes = http.post(
            `${data.baseUrl}/extractions`,
            JSON.stringify(extractionConfig),
            {
                headers: headers,
                tags: {
                    name: 'CreateExtraction',
                    phase: currentPhase,
                },
                timeout: '30s',
            }
        );
        const duration = Date.now() - startTime;

        // Record latency by phase
        extractionLatency.add(duration);
        if (currentPhase === 'spike') {
            extractionLatencyDuringSpike.add(duration);
        } else if (currentPhase === 'recovery') {
            extractionLatencyAfterSpike.add(duration);
        }

        const createSuccess = check(createRes, {
            'create succeeded': (r) => r.status === 201 || r.status === 200,
        });

        if (createSuccess) {
            const body = createRes.json();
            extractionId = body.id || body.data?.id;
            extractionsCreated.add(1);
            extractionErrors.add(0);
        } else {
            extractionErrors.add(1);

            if (currentPhase === 'spike') {
                errorsDuringSpike.add(1);
            } else if (currentPhase === 'recovery') {
                errorsAfterSpike.add(1);
            }

            return;
        }
    });

    if (!extractionId) {
        return;
    }

    // 2. Start extraction
    group('Start Extraction', function () {
        const startRes = http.post(
            `${data.baseUrl}/extractions/${extractionId}/start`,
            null,
            {
                headers: headers,
                tags: { name: 'StartExtraction', phase: currentPhase },
                timeout: '30s',
            }
        );

        check(startRes, {
            'start succeeded': (r) => r.status === 200 || r.status === 202,
        });
    });

    // 3. Quick status check (minimal polling for spike test)
    group('Check Status', function () {
        const statusRes = http.get(
            `${data.baseUrl}/extractions/${extractionId}`,
            {
                headers: headers,
                tags: { name: 'GetStatus', phase: currentPhase },
                timeout: '10s',
            }
        );

        check(statusRes, {
            'status retrieved': (r) => r.status === 200,
        });
    });

    // Variable think time based on phase
    if (currentPhase === 'spike') {
        sleep(0.2); // Minimal sleep during spike
    } else if (currentPhase === 'recovery') {
        sleep(1); // Normal sleep during recovery
    } else {
        sleep(1.5); // Comfortable sleep during normal load
    }
}

/**
 * Teardown function
 */
export function teardown(data) {
    console.log('');
    console.log('==================================================');
    console.log('Spike Test Completed');
    console.log('==================================================');
}

/**
 * Custom summary report
 */
export function handleSummary(data) {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');

    const totalRequests = data.metrics.http_reqs?.values?.count || 0;
    const failedRequests = data.metrics.http_req_failed?.values?.passes || 0;
    const errorsDuringSpike = data.metrics.errors_during_spike?.values?.count || 0;
    const errorsAfterSpike = data.metrics.errors_after_spike?.values?.count || 0;

    const summary = {
        timestamp: timestamp,
        test_type: 'spike',
        normal_load: NORMAL_LOAD,
        spike_load: SPIKE_LOAD,
        spike_multiplier: SPIKE_MULTIPLIER,
        duration_seconds: data.state.testRunDurationMs / 1000,
        metrics: {
            http: {
                total_requests: totalRequests,
                failed_requests: failedRequests,
                failure_rate: totalRequests > 0 ? ((failedRequests / totalRequests) * 100).toFixed(2) : 0,
                avg_duration_ms: data.metrics.http_req_duration?.values?.avg?.toFixed(2) || 0,
                p95_duration_ms: data.metrics.http_req_duration?.values['p(95)']?.toFixed(2) || 0,
                p99_duration_ms: data.metrics.http_req_duration?.values['p(99)']?.toFixed(2) || 0,
            },
            extraction: {
                created: data.metrics.extractions_created?.values?.count || 0,
                overall_latency_avg: data.metrics.extraction_latency?.values?.avg?.toFixed(2) || 0,
                overall_latency_p95: data.metrics.extraction_latency?.values['p(95)']?.toFixed(2) || 0,
            },
            spike_behavior: {
                latency_during_spike_avg: data.metrics.extraction_latency_during_spike?.values?.avg?.toFixed(2) || 0,
                latency_during_spike_p95: data.metrics.extraction_latency_during_spike?.values['p(95)']?.toFixed(2) || 0,
                latency_during_spike_max: data.metrics.extraction_latency_during_spike?.values?.max?.toFixed(2) || 0,
                errors_during_spike: errorsDuringSpike,
            },
            recovery_behavior: {
                latency_after_spike_avg: data.metrics.extraction_latency_after_spike?.values?.avg?.toFixed(2) || 0,
                latency_after_spike_p95: data.metrics.extraction_latency_after_spike?.values['p(95)']?.toFixed(2) || 0,
                errors_after_spike: errorsAfterSpike,
            },
        },
        analysis: {
            spike_degradation_percent: 0,
            recovery_successful: false,
            auto_scaling_effective: false,
        },
    };

    // Calculate spike degradation
    if (summary.metrics.extraction.overall_latency_avg > 0) {
        summary.analysis.spike_degradation_percent =
            ((summary.metrics.spike_behavior.latency_during_spike_avg /
              summary.metrics.extraction.overall_latency_avg) * 100).toFixed(2);
    }

    // Check if recovery was successful
    summary.analysis.recovery_successful =
        summary.metrics.recovery_behavior.latency_after_spike_p95 <
        summary.metrics.spike_behavior.latency_during_spike_p95 * 0.6;

    console.log('\n' + '='.repeat(70));
    console.log('SPIKE TEST RESULTS');
    console.log('='.repeat(70));
    console.log(`\nLoad Configuration:`);
    console.log(`  Normal Load: ${NORMAL_LOAD} VUs`);
    console.log(`  Spike Load: ${SPIKE_LOAD} VUs (${SPIKE_MULTIPLIER}x increase)`);
    console.log(`\nTotal Requests: ${summary.metrics.http.total_requests}`);
    console.log(`Failed Requests: ${summary.metrics.http.failed_requests} (${summary.metrics.http.failure_rate}%)`);
    console.log(`\nOVERALL LATENCY:`);
    console.log(`  Average: ${summary.metrics.extraction.overall_latency_avg}ms`);
    console.log(`  p95: ${summary.metrics.extraction.overall_latency_p95}ms`);
    console.log(`\nDURING SPIKE:`);
    console.log(`  Latency (avg): ${summary.metrics.spike_behavior.latency_during_spike_avg}ms`);
    console.log(`  Latency (p95): ${summary.metrics.spike_behavior.latency_during_spike_p95}ms`);
    console.log(`  Latency (max): ${summary.metrics.spike_behavior.latency_during_spike_max}ms`);
    console.log(`  Errors: ${summary.metrics.spike_behavior.errors_during_spike}`);
    console.log(`  Degradation: ${summary.analysis.spike_degradation_percent}%`);
    console.log(`\nAFTER SPIKE (RECOVERY):`);
    console.log(`  Latency (avg): ${summary.metrics.recovery_behavior.latency_after_spike_avg}ms`);
    console.log(`  Latency (p95): ${summary.metrics.recovery_behavior.latency_after_spike_p95}ms`);
    console.log(`  Errors: ${summary.metrics.recovery_behavior.errors_after_spike}`);
    console.log(`\nRECOVERY ANALYSIS:`);

    if (summary.analysis.recovery_successful) {
        console.log(`  ✓ System recovered successfully after spike`);
        console.log(`  ✓ Latency returned to acceptable levels`);
    } else {
        console.log(`  ⚠ System did not fully recover after spike`);
        console.log(`  → Consider tuning connection pools or implementing circuit breakers`);
    }

    if (summary.metrics.spike_behavior.errors_during_spike > 0) {
        console.log(`  ⚠ ${summary.metrics.spike_behavior.errors_during_spike} errors occurred during spike`);
        console.log(`  → Consider implementing rate limiting or request queuing`);
    } else {
        console.log(`  ✓ No errors during spike - system handled load well`);
    }

    if (summary.metrics.recovery_behavior.errors_after_spike > 0) {
        console.log(`  ⚠ ${summary.metrics.recovery_behavior.errors_after_spike} errors during recovery`);
        console.log(`  → System may need more time to recover or has lingering issues`);
    } else {
        console.log(`  ✓ Clean recovery - no errors after spike subsided`);
    }

    console.log(`\nRECOMMENDATIONS:`);
    if (summary.analysis.spike_degradation_percent > 300) {
        console.log(`  → Latency increased ${summary.analysis.spike_degradation_percent}% during spike`);
        console.log(`  → Consider implementing auto-scaling or request queuing`);
    } else if (summary.analysis.spike_degradation_percent > 200) {
        console.log(`  → Moderate degradation during spike`);
        console.log(`  → Connection pool tuning may help`);
    } else {
        console.log(`  ✓ System handled spike well with minimal degradation`);
    }

    console.log('='.repeat(70) + '\n');

    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        [`load-test-results/spike-${timestamp}.json`]: JSON.stringify(summary, null, 2),
        [`load-test-results/spike-${timestamp}-full.json`]: JSON.stringify(data, null, 2),
    };
}
