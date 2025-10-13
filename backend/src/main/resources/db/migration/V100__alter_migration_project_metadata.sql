-- Change project_metadata column from JSONB to TEXT to fix Hibernate mapping issues
-- This allows Hibernate to properly handle String to JSON conversion

ALTER TABLE migration_projects
    ALTER COLUMN project_metadata TYPE TEXT USING project_metadata::TEXT;

-- Add comment explaining the column stores JSON as text
COMMENT ON COLUMN migration_projects.project_metadata IS 'Stores JSON metadata as TEXT (was JSONB)';
