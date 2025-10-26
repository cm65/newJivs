-- Migration V113: Prepare for password encryption in data_sources table
-- ============================================================================
-- Purpose: Add infrastructure for migrating plaintext passwords to encrypted
--          passwords. This is a critical security fix.
--
-- WARNING: This migration does NOT encrypt passwords directly in SQL.
--          Encryption is handled by DataSourcePasswordMigration.java on startup.
--
-- Migration Strategy:
-- 1. Add temporary column to store plaintext passwords during migration
-- 2. Copy current passwords to temp column
-- 3. Add migration status tracking column
-- 4. Create audit table for migration tracking
-- 5. Application will encrypt passwords on startup
-- 6. After successful encryption, temp column is dropped
--
-- Author: jivs-extraction-expert
-- Date: 2025-10-26
-- Related: EXTRACTION_MODULE_FIXES.md - Issue #2
-- ============================================================================

-- Step 1: Add temporary column for plaintext passwords
-- This allows rollback if encryption fails
ALTER TABLE data_sources ADD COLUMN IF NOT EXISTS password_plaintext_temp VARCHAR(500);

-- Step 2: Copy current passwords to temp column
-- Only copy if not already copied (allows re-running migration)
UPDATE data_sources
SET password_plaintext_temp = password_encrypted
WHERE password_plaintext_temp IS NULL
  AND password_encrypted IS NOT NULL;

-- Step 3: Add migration status column
-- Tracks which records have been encrypted
ALTER TABLE data_sources ADD COLUMN IF NOT EXISTS password_migration_status VARCHAR(20) DEFAULT 'PENDING';

-- Set status to PENDING for all records with passwords
UPDATE data_sources
SET password_migration_status = 'PENDING'
WHERE password_plaintext_temp IS NOT NULL
  AND password_migration_status IS NULL;

-- Step 4: Create audit table for migration tracking
-- Tracks success/failure for each data source
CREATE TABLE IF NOT EXISTS password_migration_audit (
    id BIGSERIAL PRIMARY KEY,
    data_source_id BIGINT NOT NULL REFERENCES data_sources(id),
    migration_status VARCHAR(20) NOT NULL,
    migrated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    error_message TEXT,
    encrypted_password_length INT,

    -- Indexes for monitoring
    CONSTRAINT password_migration_audit_status_check
        CHECK (migration_status IN ('SUCCESS', 'FAILED', 'SKIPPED'))
);

-- Add index for quick status lookups
CREATE INDEX IF NOT EXISTS idx_password_migration_audit_status
    ON password_migration_audit(migration_status);

CREATE INDEX IF NOT EXISTS idx_password_migration_audit_data_source
    ON password_migration_audit(data_source_id);

-- Step 5: Create view for migration monitoring
CREATE OR REPLACE VIEW password_migration_status_summary AS
SELECT
    password_migration_status,
    COUNT(*) as count,
    MIN(created_at) as earliest_source,
    MAX(created_at) as latest_source
FROM data_sources
WHERE password_plaintext_temp IS NOT NULL
GROUP BY password_migration_status;

-- Step 6: Add helpful comments
COMMENT ON COLUMN data_sources.password_plaintext_temp IS
    'Temporary column for password migration. Will be dropped after encryption completes.';

COMMENT ON COLUMN data_sources.password_migration_status IS
    'Migration status: PENDING, COMPLETED, FAILED, ROLLBACK';

COMMENT ON TABLE password_migration_audit IS
    'Audit log for password encryption migration. Tracks success/failure per data source.';

-- Step 7: Grant permissions (if using role-based access)
-- Uncomment if you have specific database roles
-- GRANT SELECT, UPDATE ON data_sources TO jivs_application;
-- GRANT SELECT, INSERT ON password_migration_audit TO jivs_application;

-- ============================================================================
-- POST-MIGRATION VERIFICATION QUERIES
-- ============================================================================
-- Run these after migration to verify success:
--
-- 1. Check migration status:
--    SELECT * FROM password_migration_status_summary;
--
-- 2. View pending migrations:
--    SELECT id, name, password_migration_status
--    FROM data_sources
--    WHERE password_migration_status = 'PENDING';
--
-- 3. Check for failures:
--    SELECT * FROM password_migration_audit
--    WHERE migration_status = 'FAILED';
--
-- 4. Verify encrypted password lengths (should be >50 chars):
--    SELECT id, name,
--           length(password_encrypted) as encrypted_len,
--           password_migration_status
--    FROM data_sources
--    WHERE password_encrypted IS NOT NULL;
--
-- ============================================================================
-- ROLLBACK PROCEDURE
-- ============================================================================
-- If migration fails and you need to rollback:
--
-- 1. Restore passwords from temp column:
--    UPDATE data_sources
--    SET password_encrypted = password_plaintext_temp,
--        password_migration_status = 'ROLLBACK'
--    WHERE password_migration_status = 'FAILED';
--
-- 2. Drop migration infrastructure (ONLY AFTER SUCCESSFUL MIGRATION):
--    ALTER TABLE data_sources DROP COLUMN password_plaintext_temp;
--    ALTER TABLE data_sources DROP COLUMN password_migration_status;
--    DROP TABLE password_migration_audit;
--    DROP VIEW password_migration_status_summary;
--
-- ============================================================================
-- SECURITY NOTES
-- ============================================================================
--
-- IMPORTANT: password_plaintext_temp contains sensitive data!
--
-- 1. This column should be dropped immediately after successful encryption
-- 2. Database backups taken during migration contain plaintext passwords
-- 3. Encrypt old backups or rotate passwords after migration
-- 4. Monitor password_migration_audit for unauthorized access attempts
-- 5. Application must use CryptoUtil with secure key management
--
-- Key Management:
-- - Encryption key must be in environment variable or secrets manager
-- - Never commit encryption key to source control
-- - Rotate keys periodically and re-encrypt
--
-- ============================================================================

-- Final verification: Ensure migration setup is complete
DO $$
DECLARE
    temp_column_exists BOOLEAN;
    status_column_exists BOOLEAN;
    audit_table_exists BOOLEAN;
    pending_count INTEGER;
BEGIN
    -- Check if required columns exist
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='data_sources'
        AND column_name='password_plaintext_temp'
    ) INTO temp_column_exists;

    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='data_sources'
        AND column_name='password_migration_status'
    ) INTO status_column_exists;

    SELECT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_name='password_migration_audit'
    ) INTO audit_table_exists;

    -- Get count of records pending migration
    SELECT COUNT(*) INTO pending_count
    FROM data_sources
    WHERE password_migration_status = 'PENDING';

    -- Log migration setup status
    RAISE NOTICE '=== Password Migration Setup Complete ===';
    RAISE NOTICE 'Temp column exists: %', temp_column_exists;
    RAISE NOTICE 'Status column exists: %', status_column_exists;
    RAISE NOTICE 'Audit table exists: %', audit_table_exists;
    RAISE NOTICE 'Records pending encryption: %', pending_count;
    RAISE NOTICE '';
    RAISE NOTICE 'Next step: Deploy application with DataSourcePasswordMigration';
    RAISE NOTICE 'Application will encrypt passwords on startup';
END $$;
