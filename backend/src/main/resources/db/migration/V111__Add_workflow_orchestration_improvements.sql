-- Migration V111: Workflow Orchestration Improvements
-- Date: 2025-10-26
-- Description: Add checkpoint support, improve migration tracking, add distributed lock metadata

-- ================================
-- 1. Add checkpoint tracking table
-- ================================

CREATE TABLE IF NOT EXISTS migration_checkpoints (
    id BIGSERIAL PRIMARY KEY,
    migration_id BIGINT NOT NULL REFERENCES migrations(id) ON DELETE CASCADE,
    phase VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Snapshot of metrics at checkpoint time
    total_records INTEGER,
    processed_records INTEGER,
    successful_records INTEGER,
    failed_records INTEGER,

    -- Checkpoint metadata
    checkpoint_data JSONB,

    -- Indexes
    CONSTRAINT fk_migration_checkpoint FOREIGN KEY (migration_id) REFERENCES migrations(id) ON DELETE CASCADE
);

CREATE INDEX idx_migration_checkpoints_migration_id ON migration_checkpoints(migration_id);
CREATE INDEX idx_migration_checkpoints_phase ON migration_checkpoints(phase);
CREATE INDEX idx_migration_checkpoints_created_at ON migration_checkpoints(created_at DESC);

COMMENT ON TABLE migration_checkpoints IS 'Stores checkpoints for migration phases to enable resume functionality';
COMMENT ON COLUMN migration_checkpoints.checkpoint_data IS 'JSON data containing phase-specific checkpoint information';

-- ================================
-- 2. Add resume tracking columns
-- ================================

ALTER TABLE migrations ADD COLUMN IF NOT EXISTS resumed_time TIMESTAMP;
ALTER TABLE migrations ADD COLUMN IF NOT EXISTS resume_count INTEGER DEFAULT 0;
ALTER TABLE migrations ADD COLUMN IF NOT EXISTS last_checkpoint_phase VARCHAR(50);

COMMENT ON COLUMN migrations.resumed_time IS 'Timestamp when migration was last resumed';
COMMENT ON COLUMN migrations.resume_count IS 'Number of times migration has been resumed';
COMMENT ON COLUMN migrations.last_checkpoint_phase IS 'Last phase where checkpoint was created';

-- ================================
-- 3. Add distributed lock tracking
-- ================================

CREATE TABLE IF NOT EXISTS migration_locks (
    id BIGSERIAL PRIMARY KEY,
    migration_id BIGINT NOT NULL UNIQUE REFERENCES migrations(id) ON DELETE CASCADE,
    lock_key VARCHAR(255) NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    locked_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    lock_metadata JSONB,

    CONSTRAINT fk_migration_lock FOREIGN KEY (migration_id) REFERENCES migrations(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_migration_locks_migration_id ON migration_locks(migration_id);
CREATE INDEX idx_migration_locks_expires_at ON migration_locks(expires_at);
CREATE INDEX idx_migration_locks_locked_by ON migration_locks(locked_by);

COMMENT ON TABLE migration_locks IS 'Tracks distributed locks for migration operations';
COMMENT ON COLUMN migration_locks.locked_by IS 'Identifier of the service instance holding the lock';
COMMENT ON COLUMN migration_locks.expires_at IS 'Lock expiration time for automatic release';

-- ================================
-- 4. Add retry tracking
-- ================================

CREATE TABLE IF NOT EXISTS migration_retries (
    id BIGSERIAL PRIMARY KEY,
    migration_id BIGINT NOT NULL REFERENCES migrations(id) ON DELETE CASCADE,
    phase VARCHAR(50) NOT NULL,
    attempt_number INTEGER NOT NULL,
    retry_reason TEXT,
    retry_time TIMESTAMP NOT NULL DEFAULT NOW(),
    success BOOLEAN,
    error_message TEXT,

    CONSTRAINT fk_migration_retry FOREIGN KEY (migration_id) REFERENCES migrations(id) ON DELETE CASCADE
);

CREATE INDEX idx_migration_retries_migration_id ON migration_retries(migration_id);
CREATE INDEX idx_migration_retries_phase ON migration_retries(phase);
CREATE INDEX idx_migration_retries_retry_time ON migration_retries(retry_time DESC);

COMMENT ON TABLE migration_retries IS 'Tracks retry attempts for failed migration phases';

-- ================================
-- 5. Add event publishing failures
-- ================================

CREATE TABLE IF NOT EXISTS migration_event_failures (
    id BIGSERIAL PRIMARY KEY,
    migration_id BIGINT NOT NULL REFERENCES migrations(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    event_data JSONB NOT NULL,
    failure_time TIMESTAMP NOT NULL DEFAULT NOW(),
    retry_count INTEGER DEFAULT 0,
    last_retry_time TIMESTAMP,
    resolved BOOLEAN DEFAULT FALSE,
    resolved_time TIMESTAMP,
    error_message TEXT,

    CONSTRAINT fk_migration_event_failure FOREIGN KEY (migration_id) REFERENCES migrations(id) ON DELETE CASCADE
);

CREATE INDEX idx_migration_event_failures_migration_id ON migration_event_failures(migration_id);
CREATE INDEX idx_migration_event_failures_resolved ON migration_event_failures(resolved) WHERE resolved = FALSE;
CREATE INDEX idx_migration_event_failures_failure_time ON migration_event_failures(failure_time DESC);

COMMENT ON TABLE migration_event_failures IS 'Tracks failed event publishing attempts for retry';

-- ================================
-- 6. Add idempotency tracking
-- ================================

CREATE TABLE IF NOT EXISTS migration_idempotency_keys (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    migration_id BIGINT NOT NULL REFERENCES migrations(id) ON DELETE CASCADE,
    operation_type VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_migration_idempotency FOREIGN KEY (migration_id) REFERENCES migrations(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_migration_idempotency_keys_key ON migration_idempotency_keys(idempotency_key);
CREATE INDEX idx_migration_idempotency_keys_expires_at ON migration_idempotency_keys(expires_at);
CREATE INDEX idx_migration_idempotency_keys_migration_id ON migration_idempotency_keys(migration_id);

COMMENT ON TABLE migration_idempotency_keys IS 'Ensures idempotent processing of migration messages';

-- ================================
-- 7. Add performance metrics table
-- ================================

CREATE TABLE IF NOT EXISTS migration_performance_metrics (
    id BIGSERIAL PRIMARY KEY,
    migration_id BIGINT NOT NULL REFERENCES migrations(id) ON DELETE CASCADE,
    phase VARCHAR(50) NOT NULL,

    -- Timing metrics
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration_seconds INTEGER,

    -- Throughput metrics
    records_per_second DECIMAL(10, 2),
    bytes_processed BIGINT,
    bytes_per_second DECIMAL(10, 2),

    -- Resource metrics
    avg_cpu_percent DECIMAL(5, 2),
    avg_memory_mb INTEGER,
    peak_memory_mb INTEGER,

    -- Thread pool metrics
    avg_thread_utilization DECIMAL(5, 2),
    peak_thread_count INTEGER,

    CONSTRAINT fk_migration_performance FOREIGN KEY (migration_id) REFERENCES migrations(id) ON DELETE CASCADE
);

CREATE INDEX idx_migration_performance_migration_id ON migration_performance_metrics(migration_id);
CREATE INDEX idx_migration_performance_phase ON migration_performance_metrics(phase);
CREATE INDEX idx_migration_performance_start_time ON migration_performance_metrics(start_time DESC);

COMMENT ON TABLE migration_performance_metrics IS 'Detailed performance metrics for each migration phase';

-- ================================
-- 8. Add circuit breaker state tracking
-- ================================

CREATE TABLE IF NOT EXISTS circuit_breaker_events (
    id BIGSERIAL PRIMARY KEY,
    circuit_breaker_name VARCHAR(100) NOT NULL,
    event_type VARCHAR(50) NOT NULL, -- OPEN, CLOSED, HALF_OPEN, FAILURE, SUCCESS
    event_time TIMESTAMP NOT NULL DEFAULT NOW(),
    failure_rate DECIMAL(5, 2),
    slow_call_rate DECIMAL(5, 2),
    metadata JSONB
);

CREATE INDEX idx_circuit_breaker_events_name ON circuit_breaker_events(circuit_breaker_name);
CREATE INDEX idx_circuit_breaker_events_type ON circuit_breaker_events(event_type);
CREATE INDEX idx_circuit_breaker_events_time ON circuit_breaker_events(event_time DESC);

COMMENT ON TABLE circuit_breaker_events IS 'Tracks circuit breaker state transitions for debugging';

-- ================================
-- 9. Add cleanup job for old data
-- ================================

-- Function to clean up old checkpoint data
CREATE OR REPLACE FUNCTION cleanup_old_migration_data() RETURNS void AS $$
BEGIN
    -- Clean up checkpoints older than 90 days
    DELETE FROM migration_checkpoints WHERE created_at < NOW() - INTERVAL '90 days';

    -- Clean up resolved event failures older than 30 days
    DELETE FROM migration_event_failures WHERE resolved = TRUE AND resolved_time < NOW() - INTERVAL '30 days';

    -- Clean up expired idempotency keys
    DELETE FROM migration_idempotency_keys WHERE expires_at < NOW();

    -- Clean up old lock records (should be auto-cleaned by Redis, but keep DB clean)
    DELETE FROM migration_locks WHERE expires_at < NOW() - INTERVAL '1 day';

    -- Clean up old circuit breaker events
    DELETE FROM circuit_breaker_events WHERE event_time < NOW() - INTERVAL '60 days';

    -- Clean up old retry records
    DELETE FROM migration_retries WHERE retry_time < NOW() - INTERVAL '90 days';

    RAISE NOTICE 'Old migration data cleanup completed';
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_old_migration_data() IS 'Cleans up old migration tracking data to prevent unbounded growth';

-- ================================
-- 10. Add materialized view for migration statistics
-- ================================

CREATE MATERIALIZED VIEW IF NOT EXISTS migration_statistics AS
SELECT
    DATE(m.created_date) as date,
    m.status,
    m.phase,
    COUNT(*) as count,
    AVG(EXTRACT(EPOCH FROM (m.completion_time - m.start_time))) as avg_duration_seconds,
    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (m.completion_time - m.start_time))) as median_duration_seconds,
    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (m.completion_time - m.start_time))) as p95_duration_seconds,
    SUM(CASE WHEN m.status = 'COMPLETED' THEN 1 ELSE 0 END)::float / COUNT(*) * 100 as success_rate,
    AVG(m.metrics->>'totalRecords'::text)::numeric as avg_records_processed
FROM migrations m
WHERE m.created_date >= NOW() - INTERVAL '90 days'
GROUP BY DATE(m.created_date), m.status, m.phase;

CREATE UNIQUE INDEX idx_migration_statistics_date_status_phase ON migration_statistics(date, status, phase);

COMMENT ON MATERIALIZED VIEW migration_statistics IS 'Daily aggregated statistics for migration performance analysis';

-- Refresh function
CREATE OR REPLACE FUNCTION refresh_migration_statistics() RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY migration_statistics;
    RAISE NOTICE 'Migration statistics refreshed';
END;
$$ LANGUAGE plpgsql;

-- ================================
-- 11. Add indexes for performance
-- ================================

-- Improve query performance for active migrations
CREATE INDEX IF NOT EXISTS idx_migrations_status_phase ON migrations(status, phase) WHERE status IN ('IN_PROGRESS', 'PAUSED');

-- Improve query performance for recent migrations
CREATE INDEX IF NOT EXISTS idx_migrations_created_date_desc ON migrations(created_date DESC);

-- Improve query performance for migration metrics
CREATE INDEX IF NOT EXISTS idx_migrations_metrics_gin ON migrations USING gin(metrics);

-- ================================
-- 12. Add constraints
-- ================================

-- Ensure phase transitions are valid
ALTER TABLE migrations
    ADD CONSTRAINT check_valid_phase_transition
    CHECK (
        (phase = 'PLANNING' AND status IN ('INITIALIZED', 'IN_PROGRESS', 'PAUSED', 'FAILED')) OR
        (phase = 'EXTRACTION' AND status IN ('IN_PROGRESS', 'PAUSED', 'FAILED')) OR
        (phase = 'TRANSFORMATION' AND status IN ('IN_PROGRESS', 'PAUSED', 'FAILED')) OR
        (phase = 'VALIDATION' AND status IN ('IN_PROGRESS', 'PAUSED', 'FAILED')) OR
        (phase = 'LOADING' AND status IN ('IN_PROGRESS', 'PAUSED', 'FAILED')) OR
        (phase = 'VERIFICATION' AND status IN ('IN_PROGRESS', 'PAUSED', 'FAILED')) OR
        (phase = 'CLEANUP' AND status IN ('IN_PROGRESS', 'FAILED', 'COMPLETED')) OR
        (phase = 'COMPLETED' AND status = 'COMPLETED')
    );

-- ================================
-- 13. Insert initial data
-- ================================

-- No initial data needed

-- ================================
-- 14. Grant permissions
-- ================================

GRANT SELECT, INSERT, UPDATE, DELETE ON migration_checkpoints TO jivs_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON migration_locks TO jivs_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON migration_retries TO jivs_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON migration_event_failures TO jivs_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON migration_idempotency_keys TO jivs_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON migration_performance_metrics TO jivs_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON circuit_breaker_events TO jivs_user;
GRANT SELECT ON migration_statistics TO jivs_user;

GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO jivs_user;

-- ================================
-- Migration complete
-- ================================

-- Log completion
DO $$
BEGIN
    RAISE NOTICE 'Migration V111 completed successfully';
    RAISE NOTICE 'Added: checkpoint tracking, distributed lock tracking, retry tracking, event failure tracking';
    RAISE NOTICE 'Added: idempotency keys, performance metrics, circuit breaker events';
    RAISE NOTICE 'Added: materialized view for statistics';
END $$;
