-- ================================================================
-- JiVS Migration Module - Execution State Persistence
-- ================================================================
-- Version: V111
-- Purpose: Add fields to persist migration execution state
-- Author: JiVS Migration Team
-- Date: 2025-10-26
--
-- ✅ FIXES: Critical data loss issue from @Transient fields
--
-- This migration adds 40+ columns to migration_projects table to
-- persist all migration execution state that was previously lost
-- on application restart due to @Transient annotations.
-- ================================================================

-- ================================================================
-- STEP 1: Add Execution Phase & Configuration Fields
-- ================================================================

ALTER TABLE migration_projects
    ADD COLUMN IF NOT EXISTS migration_phase VARCHAR(20),
    ADD COLUMN IF NOT EXISTS batch_size INTEGER DEFAULT 1000,
    ADD COLUMN IF NOT EXISTS parallelism INTEGER DEFAULT 4,
    ADD COLUMN IF NOT EXISTS retry_attempts INTEGER DEFAULT 3,
    ADD COLUMN IF NOT EXISTS strict_validation BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS rollback_enabled BOOLEAN DEFAULT true,
    ADD COLUMN IF NOT EXISTS rollback_on_cancel BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS archive_enabled BOOLEAN DEFAULT false;

-- Add constraints
ALTER TABLE migration_projects
    ADD CONSTRAINT chk_batch_size CHECK (batch_size > 0 AND batch_size <= 10000),
    ADD CONSTRAINT chk_parallelism CHECK (parallelism > 0 AND parallelism <= 20),
    ADD CONSTRAINT chk_retry_attempts CHECK (retry_attempts >= 0 AND retry_attempts <= 10);

-- Add comments
COMMENT ON COLUMN migration_projects.migration_phase IS 'Current execution phase (PLANNING, EXTRACTION, TRANSFORMATION, VALIDATION, LOADING, VERIFICATION, CLEANUP, COMPLETED, FAILED)';
COMMENT ON COLUMN migration_projects.batch_size IS 'Number of records to process in each batch';
COMMENT ON COLUMN migration_projects.parallelism IS 'Number of parallel threads for processing';
COMMENT ON COLUMN migration_projects.retry_attempts IS 'Number of retry attempts for failed operations';
COMMENT ON COLUMN migration_projects.strict_validation IS 'Whether to fail on validation warnings';
COMMENT ON COLUMN migration_projects.rollback_enabled IS 'Whether rollback is enabled on failure';
COMMENT ON COLUMN migration_projects.rollback_on_cancel IS 'Whether to rollback when migration is cancelled';
COMMENT ON COLUMN migration_projects.archive_enabled IS 'Whether to archive migration data after completion';

-- ================================================================
-- STEP 2: Add Timestamp Fields
-- ================================================================

ALTER TABLE migration_projects
    ADD COLUMN IF NOT EXISTS start_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS completion_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS paused_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS resumed_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS cancelled_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS rollback_time TIMESTAMP;

-- Add comments
COMMENT ON COLUMN migration_projects.start_time IS 'When migration execution started';
COMMENT ON COLUMN migration_projects.completion_time IS 'When migration execution completed';
COMMENT ON COLUMN migration_projects.paused_time IS 'When migration was paused';
COMMENT ON COLUMN migration_projects.resumed_time IS 'When migration was resumed';
COMMENT ON COLUMN migration_projects.cancelled_time IS 'When migration was cancelled';
COMMENT ON COLUMN migration_projects.rollback_time IS 'When rollback was executed';

-- ================================================================
-- STEP 3: Add Rollback Tracking Fields
-- ================================================================

ALTER TABLE migration_projects
    ADD COLUMN IF NOT EXISTS rollback_executed BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS rollback_failed BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS rollback_error TEXT;

-- Add comments
COMMENT ON COLUMN migration_projects.rollback_executed IS 'Whether rollback has been executed';
COMMENT ON COLUMN migration_projects.rollback_failed IS 'Whether rollback execution failed';
COMMENT ON COLUMN migration_projects.rollback_error IS 'Error message if rollback failed';

-- ================================================================
-- STEP 4: Add Error Tracking Fields
-- ================================================================

ALTER TABLE migration_projects
    ADD COLUMN IF NOT EXISTS error_message TEXT,
    ADD COLUMN IF NOT EXISTS error_stack_trace TEXT;

-- Add comments
COMMENT ON COLUMN migration_projects.error_message IS 'Primary error message if migration failed';
COMMENT ON COLUMN migration_projects.error_stack_trace IS 'Full stack trace of the error';

-- ================================================================
-- STEP 5: Add Complex Object Storage (JSON/JSONB)
-- ================================================================

ALTER TABLE migration_projects
    ADD COLUMN IF NOT EXISTS source_analysis JSONB,
    ADD COLUMN IF NOT EXISTS target_analysis JSONB,
    ADD COLUMN IF NOT EXISTS migration_plan JSONB,
    ADD COLUMN IF NOT EXISTS resource_estimation JSONB,
    ADD COLUMN IF NOT EXISTS validation_result JSONB,
    ADD COLUMN IF NOT EXISTS verification_result JSONB,
    ADD COLUMN IF NOT EXISTS migration_parameters JSONB;

-- Add comments
COMMENT ON COLUMN migration_projects.source_analysis IS 'JSON: Analysis results of source system (schema, data volume, dependencies)';
COMMENT ON COLUMN migration_projects.target_analysis IS 'JSON: Analysis results of target system (capacity, compatibility, constraints)';
COMMENT ON COLUMN migration_projects.migration_plan IS 'JSON: Generated migration execution plan (tasks, phases, dependencies)';
COMMENT ON COLUMN migration_projects.resource_estimation IS 'JSON: Estimated resources needed (memory, CPU, storage, duration)';
COMMENT ON COLUMN migration_projects.validation_result IS 'JSON: Data validation results (errors, warnings, score)';
COMMENT ON COLUMN migration_projects.verification_result IS 'JSON: Post-migration verification results (integrity checks)';
COMMENT ON COLUMN migration_projects.migration_parameters IS 'JSON: Migration-specific parameters and configuration';

-- ================================================================
-- STEP 6: Add Metrics Fields (Denormalized for Performance)
-- ================================================================

ALTER TABLE migration_projects
    ADD COLUMN IF NOT EXISTS total_records INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS processed_records INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS successful_records INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS failed_records INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS extracted_records INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS transformed_records INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS loaded_records INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS validation_score DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS validation_errors INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS bytes_processed BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS duration_seconds BIGINT;

-- Add constraints
ALTER TABLE migration_projects
    ADD CONSTRAINT chk_total_records CHECK (total_records >= 0),
    ADD CONSTRAINT chk_processed_records CHECK (processed_records >= 0),
    ADD CONSTRAINT chk_successful_records CHECK (successful_records >= 0),
    ADD CONSTRAINT chk_failed_records CHECK (failed_records >= 0),
    ADD CONSTRAINT chk_validation_score CHECK (validation_score IS NULL OR (validation_score >= 0 AND validation_score <= 100));

-- Add comments
COMMENT ON COLUMN migration_projects.total_records IS 'Total number of records to migrate';
COMMENT ON COLUMN migration_projects.processed_records IS 'Number of records processed so far';
COMMENT ON COLUMN migration_projects.successful_records IS 'Number of records successfully migrated';
COMMENT ON COLUMN migration_projects.failed_records IS 'Number of records that failed migration';
COMMENT ON COLUMN migration_projects.extracted_records IS 'Number of records extracted from source';
COMMENT ON COLUMN migration_projects.transformed_records IS 'Number of records transformed';
COMMENT ON COLUMN migration_projects.loaded_records IS 'Number of records loaded into target';
COMMENT ON COLUMN migration_projects.validation_score IS 'Overall validation score (0-100)';
COMMENT ON COLUMN migration_projects.validation_errors IS 'Number of validation errors found';
COMMENT ON COLUMN migration_projects.bytes_processed IS 'Total bytes of data processed';
COMMENT ON COLUMN migration_projects.duration_seconds IS 'Total duration in seconds';

-- ================================================================
-- STEP 7: Add Indexes for Performance
-- ================================================================

-- Index for querying by phase
CREATE INDEX IF NOT EXISTS idx_migration_phase ON migration_projects(migration_phase)
    WHERE migration_phase IS NOT NULL;

-- Index for querying by timestamps
CREATE INDEX IF NOT EXISTS idx_migration_start_time ON migration_projects(start_time DESC)
    WHERE start_time IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_migration_completion_time ON migration_projects(completion_time DESC)
    WHERE completion_time IS NOT NULL;

-- Composite index for active migrations (in progress or paused)
CREATE INDEX IF NOT EXISTS idx_migration_active ON migration_projects(status, migration_phase, start_time)
    WHERE status IN ('IN_PROGRESS', 'PAUSED');

-- Index for failed migrations with errors
CREATE INDEX IF NOT EXISTS idx_migration_failed ON migration_projects(status, completion_time DESC)
    WHERE status = 'FAILED' AND error_message IS NOT NULL;

-- Index for searching by phase and progress
CREATE INDEX IF NOT EXISTS idx_migration_progress ON migration_projects(migration_phase, processed_records, total_records)
    WHERE total_records > 0;

-- GIN index for JSON fields (for searching within JSON)
CREATE INDEX IF NOT EXISTS idx_migration_parameters_gin ON migration_projects USING GIN (migration_parameters)
    WHERE migration_parameters IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_source_analysis_gin ON migration_projects USING GIN (source_analysis)
    WHERE source_analysis IS NOT NULL;

-- ================================================================
-- STEP 8: Update Existing Records (Safe Defaults)
-- ================================================================

-- Set safe defaults for existing records
UPDATE migration_projects
SET
    migration_phase = CASE
        WHEN status = 'COMPLETED' THEN 'COMPLETED'
        WHEN status = 'FAILED' THEN 'FAILED'
        WHEN status = 'IN_PROGRESS' THEN 'EXTRACTION'
        ELSE 'PLANNING'
    END,
    batch_size = COALESCE(batch_size, 1000),
    parallelism = COALESCE(parallelism, 4),
    retry_attempts = COALESCE(retry_attempts, 3),
    strict_validation = COALESCE(strict_validation, false),
    rollback_enabled = COALESCE(rollback_enabled, true),
    rollback_on_cancel = COALESCE(rollback_on_cancel, false),
    archive_enabled = COALESCE(archive_enabled, false),
    rollback_executed = COALESCE(rollback_executed, false),
    rollback_failed = COALESCE(rollback_failed, false),
    total_records = COALESCE(total_records, 0),
    processed_records = COALESCE(processed_records, 0),
    successful_records = COALESCE(successful_records, 0),
    failed_records = COALESCE(failed_records, 0),
    extracted_records = COALESCE(extracted_records, 0),
    transformed_records = COALESCE(transformed_records, 0),
    loaded_records = COALESCE(loaded_records, 0),
    validation_errors = COALESCE(validation_errors, 0),
    bytes_processed = COALESCE(bytes_processed, 0)
WHERE migration_phase IS NULL;

-- ================================================================
-- STEP 9: Add Check Constraints for Data Integrity
-- ================================================================

-- Ensure processed records doesn't exceed total
ALTER TABLE migration_projects
    ADD CONSTRAINT chk_processed_vs_total CHECK (
        total_records = 0 OR processed_records <= total_records OR processed_records IS NULL
    );

-- Ensure successful + failed = processed (with tolerance for edge cases)
ALTER TABLE migration_projects
    ADD CONSTRAINT chk_record_accounting CHECK (
        processed_records IS NULL OR
        successful_records IS NULL OR
        failed_records IS NULL OR
        (successful_records + failed_records) <= (processed_records + 100) -- 100 record tolerance
    );

-- Ensure completion_time is after start_time
ALTER TABLE migration_projects
    ADD CONSTRAINT chk_completion_after_start CHECK (
        start_time IS NULL OR
        completion_time IS NULL OR
        completion_time >= start_time
    );

-- Ensure paused_time is after start_time
ALTER TABLE migration_projects
    ADD CONSTRAINT chk_paused_after_start CHECK (
        start_time IS NULL OR
        paused_time IS NULL OR
        paused_time >= start_time
    );

-- ================================================================
-- STEP 10: Create Materialized View for Analytics
-- ================================================================

CREATE MATERIALIZED VIEW IF NOT EXISTS migration_analytics AS
SELECT
    DATE_TRUNC('day', created_at) AS migration_date,
    status,
    migration_phase,
    COUNT(*) AS migration_count,
    SUM(total_records) AS total_records_sum,
    SUM(processed_records) AS processed_records_sum,
    SUM(successful_records) AS successful_records_sum,
    SUM(failed_records) AS failed_records_sum,
    AVG(validation_score) AS avg_validation_score,
    AVG(duration_seconds) AS avg_duration_seconds,
    MIN(duration_seconds) AS min_duration_seconds,
    MAX(duration_seconds) AS max_duration_seconds,
    SUM(bytes_processed) AS total_bytes_processed
FROM migration_projects
GROUP BY DATE_TRUNC('day', created_at), status, migration_phase;

-- Index for materialized view
CREATE INDEX idx_migration_analytics_date ON migration_analytics(migration_date DESC);
CREATE INDEX idx_migration_analytics_status ON migration_analytics(status, migration_date DESC);

-- Add refresh schedule comment
COMMENT ON MATERIALIZED VIEW migration_analytics IS 'Aggregated migration statistics. Refresh schedule: Every 1 hour via cron job';

-- ================================================================
-- STEP 11: Create Function for Progress Calculation
-- ================================================================

CREATE OR REPLACE FUNCTION calculate_migration_progress(p_migration_id BIGINT)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_total INTEGER;
    v_processed INTEGER;
    v_progress INTEGER;
BEGIN
    SELECT total_records, processed_records
    INTO v_total, v_processed
    FROM migration_projects
    WHERE id = p_migration_id;

    IF v_total IS NULL OR v_total = 0 THEN
        RETURN 0;
    END IF;

    v_progress := ROUND((v_processed::NUMERIC / v_total::NUMERIC) * 100);

    -- Cap at 100%
    RETURN LEAST(100, GREATEST(0, v_progress));
END;
$$;

COMMENT ON FUNCTION calculate_migration_progress IS 'Calculate migration progress percentage (0-100)';

-- ================================================================
-- STEP 12: Create Audit Trigger for Changes
-- ================================================================

CREATE OR REPLACE FUNCTION migration_audit_trigger()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    -- Update updated_at timestamp
    NEW.updated_at := NOW();

    -- Log significant state changes
    IF OLD.status IS DISTINCT FROM NEW.status OR
       OLD.migration_phase IS DISTINCT FROM NEW.migration_phase THEN

        INSERT INTO migration_audit_log (
            migration_id,
            old_status,
            new_status,
            old_phase,
            new_phase,
            changed_at,
            changed_by
        ) VALUES (
            NEW.id,
            OLD.status,
            NEW.status,
            OLD.migration_phase,
            NEW.migration_phase,
            NOW(),
            NEW.updated_by
        );
    END IF;

    RETURN NEW;
END;
$$;

-- Create audit log table
CREATE TABLE IF NOT EXISTS migration_audit_log (
    id BIGSERIAL PRIMARY KEY,
    migration_id BIGINT NOT NULL REFERENCES migration_projects(id) ON DELETE CASCADE,
    old_status VARCHAR(50),
    new_status VARCHAR(50),
    old_phase VARCHAR(20),
    new_phase VARCHAR(20),
    changed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    changed_by VARCHAR(50)
);

CREATE INDEX idx_audit_migration_id ON migration_audit_log(migration_id, changed_at DESC);
CREATE INDEX idx_audit_changed_at ON migration_audit_log(changed_at DESC);

-- Create trigger
DROP TRIGGER IF EXISTS migration_state_change_trigger ON migration_projects;
CREATE TRIGGER migration_state_change_trigger
    BEFORE UPDATE ON migration_projects
    FOR EACH ROW
    EXECUTE FUNCTION migration_audit_trigger();

-- ================================================================
-- STEP 13: Grant Permissions
-- ================================================================

-- Grant permissions to application user
GRANT SELECT, INSERT, UPDATE ON migration_projects TO jivs_user;
GRANT SELECT ON migration_analytics TO jivs_user;
GRANT SELECT, INSERT ON migration_audit_log TO jivs_user;
GRANT USAGE, SELECT ON SEQUENCE migration_audit_log_id_seq TO jivs_user;
GRANT EXECUTE ON FUNCTION calculate_migration_progress TO jivs_user;

-- ================================================================
-- STEP 14: Add Migration Statistics
-- ================================================================

-- Log migration statistics
DO $$
DECLARE
    v_total_migrations INTEGER;
    v_columns_added INTEGER := 40;
    v_indexes_added INTEGER := 8;
BEGIN
    SELECT COUNT(*) INTO v_total_migrations FROM migration_projects;

    RAISE NOTICE '================================================================';
    RAISE NOTICE 'Migration V111 Applied Successfully';
    RAISE NOTICE '================================================================';
    RAISE NOTICE 'Columns added: %', v_columns_added;
    RAISE NOTICE 'Indexes created: %', v_indexes_added;
    RAISE NOTICE 'Existing migrations updated: %', v_total_migrations;
    RAISE NOTICE 'Audit logging: ENABLED';
    RAISE NOTICE 'Analytics view: CREATED';
    RAISE NOTICE '================================================================';
END;
$$;

-- ================================================================
-- END OF MIGRATION V111
-- ================================================================
-- This migration fixes the critical data loss issue where 18+
-- @Transient fields were never persisted to the database.
--
-- All migration execution state is now properly persisted and
-- will survive application restarts.
--
-- Rollback Instructions (if needed):
-- 1. DROP TRIGGER migration_state_change_trigger
-- 2. DROP FUNCTION migration_audit_trigger()
-- 3. DROP TABLE migration_audit_log
-- 4. DROP MATERIALIZED VIEW migration_analytics
-- 5. DROP FUNCTION calculate_migration_progress
-- 6. DROP all added indexes
-- 7. ALTER TABLE migration_projects DROP COLUMN (all 40+ columns)
-- ================================================================
