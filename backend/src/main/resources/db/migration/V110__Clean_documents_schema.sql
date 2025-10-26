-- V110: Clean up documents table schema
-- Remove duplicate columns from V7 migration that are no longer used
-- Add constraints and optimizations for production use

-- Drop unused V7 columns (keeping only V101 columns that JPA entity uses)
ALTER TABLE documents
DROP COLUMN IF EXISTS document_id CASCADE,
DROP COLUMN IF EXISTS document_name CASCADE,
DROP COLUMN IF EXISTS original_filename CASCADE,
DROP COLUMN IF EXISTS file_extension CASCADE,
DROP COLUMN IF EXISTS mime_type CASCADE,
DROP COLUMN IF EXISTS file_size_bytes CASCADE,
DROP COLUMN IF EXISTS storage_location CASCADE,
DROP COLUMN IF EXISTS checksum_md5 CASCADE,
DROP COLUMN IF EXISTS checksum_sha256 CASCADE,
DROP COLUMN IF EXISTS category_id CASCADE,
DROP COLUMN IF EXISTS business_object_id CASCADE,
DROP COLUMN IF EXISTS business_object_record_id CASCADE,
DROP COLUMN IF EXISTS document_status CASCADE,
DROP COLUMN IF EXISTS is_encrypted CASCADE,
DROP COLUMN IF EXISTS encryption_key_id CASCADE,
DROP COLUMN IF EXISTS content_indexed CASCADE,
DROP COLUMN IF EXISTS ocr_processed CASCADE,
DROP COLUMN IF EXISTS thumbnail_path CASCADE,
DROP COLUMN IF EXISTS created_by CASCADE,
DROP COLUMN IF EXISTS updated_by CASCADE,
DROP COLUMN IF EXISTS created_at CASCADE,
DROP COLUMN IF EXISTS updated_at CASCADE;

-- Drop unused V7 indexes
DROP INDEX IF EXISTS idx_documents_document_id;
DROP INDEX IF EXISTS idx_documents_category;
DROP INDEX IF EXISTS idx_documents_status;
DROP INDEX IF EXISTS idx_documents_business_object;
DROP INDEX IF EXISTS idx_documents_created_at;

-- Add NOT NULL constraints to critical columns
ALTER TABLE documents
ALTER COLUMN filename SET NOT NULL,
ALTER COLUMN file_type SET NOT NULL,
ALTER COLUMN size SET NOT NULL;

-- Note: storage_path can be NULL for documents not yet stored
-- It will be set when the document is actually uploaded

-- Add unique constraint on checksum for duplicate prevention
-- Only for non-null checksums (allows multiple NULL values)
-- Drop existing index first if it exists from previous failed migration
DROP INDEX IF EXISTS unique_checksum;
CREATE UNIQUE INDEX unique_checksum ON documents(checksum) WHERE checksum IS NOT NULL;

-- Add check constraint for file size (min 1 byte, max 500MB)
DO $$ BEGIN
    ALTER TABLE documents ADD CONSTRAINT check_file_size CHECK (size > 0 AND size <= 524288000);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- Add check constraint for storage tier
DO $$ BEGIN
    ALTER TABLE documents ADD CONSTRAINT check_storage_tier CHECK (storage_tier IS NULL OR storage_tier IN ('HOT', 'WARM', 'COLD'));
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- Add check constraint for status
DO $$ BEGIN
    ALTER TABLE documents ADD CONSTRAINT check_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED'));
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- Add check constraint for compression ratio (0.0 to 1.0)
DO $$ BEGIN
    ALTER TABLE documents ADD CONSTRAINT check_compression_ratio CHECK (compression_ratio IS NULL OR (compression_ratio >= 0.0 AND compression_ratio <= 1.0));
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- Add index on compressed flag for quick filtering
CREATE INDEX IF NOT EXISTS idx_documents_compressed ON documents(compressed) WHERE compressed = true;

-- Add index on encrypted flag for compliance queries
CREATE INDEX IF NOT EXISTS idx_documents_encrypted ON documents(encrypted) WHERE encrypted = true;

-- Add index on retention_date for scheduled purge jobs
CREATE INDEX IF NOT EXISTS idx_documents_retention_date ON documents(retention_date) WHERE retention_date IS NOT NULL;

-- Add composite index for common query: active + unarchived documents
CREATE INDEX IF NOT EXISTS idx_documents_active_unarchived ON documents(status, archived) WHERE status = 'ACTIVE' AND archived = false;

-- Add composite index for archiving queries
CREATE INDEX IF NOT EXISTS idx_documents_archiving ON documents(archived, storage_tier, created_date);

-- Add PostgreSQL full-text search support
ALTER TABLE documents ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- Create function to update search vector
CREATE OR REPLACE FUNCTION documents_search_vector_trigger() RETURNS trigger AS $$
begin
  new.search_vector :=
    setweight(to_tsvector('english', coalesce(new.title,'')), 'A') ||
    setweight(to_tsvector('english', coalesce(new.filename,'')), 'B') ||
    setweight(to_tsvector('english', coalesce(new.description,'')), 'C') ||
    setweight(to_tsvector('english', coalesce(new.content,'')), 'D');
  return new;
end
$$ LANGUAGE plpgsql;

-- Create trigger to auto-update search_vector
DROP TRIGGER IF EXISTS documents_search_vector_update ON documents;
CREATE TRIGGER documents_search_vector_update
  BEFORE INSERT OR UPDATE ON documents
  FOR EACH ROW EXECUTE FUNCTION documents_search_vector_trigger();

-- Update existing rows to populate search_vector
UPDATE documents SET search_vector =
  setweight(to_tsvector('english', coalesce(title,'')), 'A') ||
  setweight(to_tsvector('english', coalesce(filename,'')), 'B') ||
  setweight(to_tsvector('english', coalesce(description,'')), 'C') ||
  setweight(to_tsvector('english', coalesce(content,'')), 'D')
WHERE search_vector IS NULL;

-- Create GIN index for full-text search
CREATE INDEX IF NOT EXISTS idx_documents_search_vector ON documents USING gin(search_vector);

-- Add comment to table
COMMENT ON TABLE documents IS 'Document metadata and storage information';
COMMENT ON COLUMN documents.filename IS 'Original filename as uploaded by user';
COMMENT ON COLUMN documents.storage_path IS 'Absolute path to file on disk (UUID-based)';
COMMENT ON COLUMN documents.checksum IS 'SHA-256 checksum (base64) for integrity verification and duplicate detection';
COMMENT ON COLUMN documents.compressed IS 'True if file is GZIP compressed on disk';
COMMENT ON COLUMN documents.compression_ratio IS 'Compression ratio (0.0-1.0) where 0.28 = 28% of original size';
COMMENT ON COLUMN documents.search_vector IS 'Full-text search vector (auto-updated via trigger)';
