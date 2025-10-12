-- V3: Create data source and extraction tables
-- Data sources and extraction job management

CREATE TABLE data_sources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    source_type VARCHAR(50) NOT NULL, -- SAP, ORACLE, SQL_SERVER, POSTGRESQL, MYSQL, FILE, API
    connection_url VARCHAR(500),
    host VARCHAR(255),
    port INTEGER,
    database_name VARCHAR(100),
    username VARCHAR(100),
    password_encrypted VARCHAR(500),
    additional_properties JSONB,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_connection_test TIMESTAMP,
    last_connection_status VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

CREATE TABLE extraction_configs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    data_source_id BIGINT NOT NULL,
    business_object_id BIGINT,
    extraction_type VARCHAR(50) NOT NULL, -- FULL, INCREMENTAL, DELTA
    extraction_query TEXT,
    where_clause TEXT,
    incremental_field VARCHAR(100),
    last_extracted_value VARCHAR(255),
    batch_size INTEGER NOT NULL DEFAULT 1000,
    parallel_threads INTEGER NOT NULL DEFAULT 1,
    timeout_minutes INTEGER NOT NULL DEFAULT 60,
    retry_attempts INTEGER NOT NULL DEFAULT 3,
    schedule_expression VARCHAR(100),
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    FOREIGN KEY (data_source_id) REFERENCES data_sources(id) ON DELETE RESTRICT,
    FOREIGN KEY (business_object_id) REFERENCES business_object_definitions(id) ON DELETE SET NULL
);

CREATE TABLE extraction_jobs (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(100) NOT NULL UNIQUE,
    extraction_config_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL, -- PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    records_extracted BIGINT DEFAULT 0,
    records_failed BIGINT DEFAULT 0,
    bytes_processed BIGINT DEFAULT 0,
    error_message TEXT,
    error_stack_trace TEXT,
    extraction_params JSONB,
    execution_context JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    triggered_by VARCHAR(50),
    FOREIGN KEY (extraction_config_id) REFERENCES extraction_configs(id) ON DELETE RESTRICT
);

CREATE TABLE extraction_logs (
    id BIGSERIAL PRIMARY KEY,
    extraction_job_id BIGINT NOT NULL,
    log_level VARCHAR(20) NOT NULL, -- INFO, WARN, ERROR, DEBUG
    log_message TEXT NOT NULL,
    additional_data JSONB,
    logged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (extraction_job_id) REFERENCES extraction_jobs(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_data_sources_type ON data_sources(source_type);
CREATE INDEX idx_data_sources_active ON data_sources(is_active);
CREATE INDEX idx_extraction_configs_data_source ON extraction_configs(data_source_id);
CREATE INDEX idx_extraction_configs_enabled ON extraction_configs(is_enabled);
CREATE INDEX idx_extraction_jobs_config_id ON extraction_jobs(extraction_config_id);
CREATE INDEX idx_extraction_jobs_status ON extraction_jobs(status);
CREATE INDEX idx_extraction_jobs_start_time ON extraction_jobs(start_time);
CREATE INDEX idx_extraction_logs_job_id ON extraction_logs(extraction_job_id);
CREATE INDEX idx_extraction_logs_level ON extraction_logs(log_level);
