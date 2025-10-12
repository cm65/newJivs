-- V7: Create document archiving tables
-- Document management and archiving system

CREATE TABLE document_categories (
    id BIGSERIAL PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    parent_category_id BIGINT,
    description TEXT,
    retention_policy_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_category_id) REFERENCES document_categories(id) ON DELETE SET NULL,
    FOREIGN KEY (retention_policy_id) REFERENCES retention_policies(id) ON DELETE SET NULL
);

CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    document_id VARCHAR(100) NOT NULL UNIQUE,
    document_name VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_extension VARCHAR(20) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    storage_location VARCHAR(50) NOT NULL, -- LOCAL, S3, AZURE, GCP
    checksum_md5 VARCHAR(32),
    checksum_sha256 VARCHAR(64),
    category_id BIGINT,
    business_object_id BIGINT,
    business_object_record_id VARCHAR(255),
    document_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, ARCHIVED, DELETED
    is_encrypted BOOLEAN NOT NULL DEFAULT FALSE,
    encryption_key_id VARCHAR(100),
    content_indexed BOOLEAN NOT NULL DEFAULT FALSE,
    ocr_processed BOOLEAN NOT NULL DEFAULT FALSE,
    thumbnail_path VARCHAR(500),
    page_count INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    FOREIGN KEY (category_id) REFERENCES document_categories(id) ON DELETE SET NULL,
    FOREIGN KEY (business_object_id) REFERENCES business_object_definitions(id) ON DELETE SET NULL
);

CREATE TABLE document_metadata (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value TEXT,
    metadata_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    UNIQUE (document_id, metadata_key)
);

CREATE TABLE document_versions (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    version_number INTEGER NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    checksum_md5 VARCHAR(32),
    change_description TEXT,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    UNIQUE (document_id, version_number)
);

CREATE TABLE document_access_log (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL, -- VIEWED, DOWNLOADED, UPDATED, DELETED
    accessed_by VARCHAR(50) NOT NULL,
    accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_documents_document_id ON documents(document_id);
CREATE INDEX idx_documents_category ON documents(category_id);
CREATE INDEX idx_documents_status ON documents(document_status);
CREATE INDEX idx_documents_business_object ON documents(business_object_id, business_object_record_id);
CREATE INDEX idx_documents_created_at ON documents(created_at);
CREATE INDEX idx_document_metadata_document_id ON document_metadata(document_id);
CREATE INDEX idx_document_versions_document_id ON document_versions(document_id);
CREATE INDEX idx_document_access_log_document_id ON document_access_log(document_id);
CREATE INDEX idx_document_access_log_accessed_at ON document_access_log(accessed_at);
