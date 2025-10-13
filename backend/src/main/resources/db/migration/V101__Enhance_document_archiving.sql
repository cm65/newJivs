-- V100: Enhance document archiving system
-- Add additional columns and tables for advanced document management

-- Add missing columns to documents table if they don't exist
ALTER TABLE documents
ADD COLUMN IF NOT EXISTS filename VARCHAR(255),
ADD COLUMN IF NOT EXISTS title VARCHAR(255),
ADD COLUMN IF NOT EXISTS description TEXT,
ADD COLUMN IF NOT EXISTS path TEXT,
ADD COLUMN IF NOT EXISTS file_type VARCHAR(10),
ADD COLUMN IF NOT EXISTS size BIGINT,
ADD COLUMN IF NOT EXISTS checksum VARCHAR(64),
ADD COLUMN IF NOT EXISTS archive_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS status VARCHAR(20),
ADD COLUMN IF NOT EXISTS archived BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS content TEXT,
ADD COLUMN IF NOT EXISTS author VARCHAR(255),
ADD COLUMN IF NOT EXISTS subject VARCHAR(255),
ADD COLUMN IF NOT EXISTS keywords TEXT,
ADD COLUMN IF NOT EXISTS word_count INTEGER,
ADD COLUMN IF NOT EXISTS language VARCHAR(10),
ADD COLUMN IF NOT EXISTS accessed_date TIMESTAMP,
ADD COLUMN IF NOT EXISTS modified_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS retention_date TIMESTAMP,
ADD COLUMN IF NOT EXISTS encrypted BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS compressed BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS compression_ratio DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS storage_tier VARCHAR(20),
ADD COLUMN IF NOT EXISTS modified_date TIMESTAMP;

-- Create document_tags table for tag support
CREATE TABLE IF NOT EXISTS document_tags (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    tag VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    UNIQUE (document_id, tag)
);

-- Create archive_records table for tracking archives
CREATE TABLE IF NOT EXISTS archive_records (
    id BIGSERIAL PRIMARY KEY,
    archive_id VARCHAR(255) NOT NULL UNIQUE,
    archive_name VARCHAR(255) NOT NULL,
    archive_type VARCHAR(20) NOT NULL, -- HOT, WARM, COLD, GLACIER
    storage_path TEXT NOT NULL,
    storage_location VARCHAR(50) NOT NULL,
    original_size BIGINT NOT NULL,
    compressed_size BIGINT,
    compression_ratio DOUBLE PRECISION,
    compression_type VARCHAR(20),
    encryption_algorithm VARCHAR(50),
    encryption_key_id VARCHAR(100),
    checksum VARCHAR(64),
    document_count INTEGER NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    retention_date TIMESTAMP,
    archive_status VARCHAR(20) DEFAULT 'ACTIVE',
    last_accessed TIMESTAMP,
    access_count INTEGER DEFAULT 0,
    metadata JSONB
);

-- Create document_archive_mapping table
CREATE TABLE IF NOT EXISTS document_archive_mapping (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    archive_id VARCHAR(255) NOT NULL,
    archived_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_by VARCHAR(255),
    archive_reason TEXT,
    original_path TEXT,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    FOREIGN KEY (archive_id) REFERENCES archive_records(archive_id) ON DELETE CASCADE,
    UNIQUE (document_id, archive_id)
);

-- Create document_content table for full-text content storage
CREATE TABLE IF NOT EXISTS document_content (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL UNIQUE,
    content_text TEXT,
    extracted_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    extraction_method VARCHAR(50),
    word_count INTEGER,
    language VARCHAR(10),
    content_hash VARCHAR(64),
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

-- Create archive_retrieval_log table
CREATE TABLE IF NOT EXISTS archive_retrieval_log (
    id BIGSERIAL PRIMARY KEY,
    archive_id VARCHAR(255) NOT NULL,
    document_id BIGINT,
    retrieved_by VARCHAR(255) NOT NULL,
    retrieved_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retrieval_reason TEXT,
    retrieval_time_ms BIGINT,
    restored_to_path TEXT,
    success BOOLEAN DEFAULT TRUE,
    error_message TEXT,
    FOREIGN KEY (archive_id) REFERENCES archive_records(archive_id) ON DELETE CASCADE,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE SET NULL
);

-- Create archive_tier_migration_log table
CREATE TABLE IF NOT EXISTS archive_tier_migration_log (
    id BIGSERIAL PRIMARY KEY,
    archive_id VARCHAR(255) NOT NULL,
    from_tier VARCHAR(20) NOT NULL,
    to_tier VARCHAR(20) NOT NULL,
    migration_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    migration_reason TEXT,
    migrated_by VARCHAR(255),
    migration_time_ms BIGINT,
    success BOOLEAN DEFAULT TRUE,
    error_message TEXT,
    FOREIGN KEY (archive_id) REFERENCES archive_records(archive_id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_documents_filename ON documents(filename);
CREATE INDEX IF NOT EXISTS idx_documents_title ON documents(title);
CREATE INDEX IF NOT EXISTS idx_documents_file_type ON documents(file_type);
CREATE INDEX IF NOT EXISTS idx_documents_archived ON documents(archived);
CREATE INDEX IF NOT EXISTS idx_documents_archive_id ON documents(archive_id);
CREATE INDEX IF NOT EXISTS idx_documents_author ON documents(author);
CREATE INDEX IF NOT EXISTS idx_documents_retention_date ON documents(retention_date);
CREATE INDEX IF NOT EXISTS idx_documents_storage_tier ON documents(storage_tier);

CREATE INDEX IF NOT EXISTS idx_document_tags_tag ON document_tags(tag);
CREATE INDEX IF NOT EXISTS idx_document_tags_document_id ON document_tags(document_id);

CREATE INDEX IF NOT EXISTS idx_archive_records_archive_id ON archive_records(archive_id);
CREATE INDEX IF NOT EXISTS idx_archive_records_archive_type ON archive_records(archive_type);
CREATE INDEX IF NOT EXISTS idx_archive_records_created_date ON archive_records(created_date);
CREATE INDEX IF NOT EXISTS idx_archive_records_retention_date ON archive_records(retention_date);

CREATE INDEX IF NOT EXISTS idx_document_archive_mapping_document_id ON document_archive_mapping(document_id);
CREATE INDEX IF NOT EXISTS idx_document_archive_mapping_archive_id ON document_archive_mapping(archive_id);

CREATE INDEX IF NOT EXISTS idx_document_content_document_id ON document_content(document_id);
CREATE INDEX IF NOT EXISTS idx_archive_retrieval_log_archive_id ON archive_retrieval_log(archive_id);
CREATE INDEX IF NOT EXISTS idx_archive_tier_migration_log_archive_id ON archive_tier_migration_log(archive_id);

-- Create full-text search indexes (PostgreSQL)
CREATE INDEX IF NOT EXISTS idx_documents_content_fulltext ON documents USING gin(to_tsvector('english', content));
CREATE INDEX IF NOT EXISTS idx_document_content_fulltext ON document_content USING gin(to_tsvector('english', content_text));