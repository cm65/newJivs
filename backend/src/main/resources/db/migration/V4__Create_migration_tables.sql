-- V4: Create migration tables
-- Migration project and job management

CREATE TABLE migration_projects (
    id BIGSERIAL PRIMARY KEY,
    project_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    project_type VARCHAR(50) NOT NULL, -- DATA_MIGRATION, APP_RETIREMENT, SYSTEM_CONSOLIDATION
    source_system VARCHAR(100) NOT NULL,
    target_system VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL, -- PLANNING, IN_PROGRESS, ON_HOLD, COMPLETED, CANCELLED
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, CRITICAL
    start_date DATE,
    end_date DATE,
    planned_cutover_date DATE,
    actual_cutover_date DATE,
    estimated_records BIGINT,
    estimated_size_gb DECIMAL(10, 2),
    project_metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

CREATE TABLE migration_phases (
    id BIGSERIAL PRIMARY KEY,
    migration_project_id BIGINT NOT NULL,
    phase_name VARCHAR(100) NOT NULL,
    phase_type VARCHAR(50) NOT NULL, -- DISCOVERY, ANALYSIS, EXTRACTION, TRANSFORMATION, VALIDATION, LOADING, VERIFICATION, CUTOVER
    sequence_order INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL, -- PENDING, IN_PROGRESS, COMPLETED, FAILED
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (migration_project_id) REFERENCES migration_projects(id) ON DELETE CASCADE,
    UNIQUE (migration_project_id, sequence_order)
);

CREATE TABLE migration_jobs (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(100) NOT NULL UNIQUE,
    migration_project_id BIGINT NOT NULL,
    migration_phase_id BIGINT,
    business_object_id BIGINT,
    job_name VARCHAR(200) NOT NULL,
    job_type VARCHAR(50) NOT NULL, -- EXTRACT, TRANSFORM, LOAD, VALIDATE
    status VARCHAR(50) NOT NULL, -- PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    priority INTEGER NOT NULL DEFAULT 5,
    depends_on_job_id BIGINT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    records_processed BIGINT DEFAULT 0,
    records_succeeded BIGINT DEFAULT 0,
    records_failed BIGINT DEFAULT 0,
    bytes_processed BIGINT DEFAULT 0,
    checkpoint_data JSONB,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    job_config JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    FOREIGN KEY (migration_project_id) REFERENCES migration_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (migration_phase_id) REFERENCES migration_phases(id) ON DELETE SET NULL,
    FOREIGN KEY (business_object_id) REFERENCES business_object_definitions(id) ON DELETE SET NULL,
    FOREIGN KEY (depends_on_job_id) REFERENCES migration_jobs(id) ON DELETE SET NULL
);

CREATE TABLE migration_logs (
    id BIGSERIAL PRIMARY KEY,
    migration_job_id BIGINT NOT NULL,
    log_level VARCHAR(20) NOT NULL,
    log_message TEXT NOT NULL,
    additional_data JSONB,
    logged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (migration_job_id) REFERENCES migration_jobs(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_migration_projects_code ON migration_projects(project_code);
CREATE INDEX idx_migration_projects_status ON migration_projects(status);
CREATE INDEX idx_migration_phases_project_id ON migration_phases(migration_project_id);
CREATE INDEX idx_migration_phases_status ON migration_phases(status);
CREATE INDEX idx_migration_jobs_project_id ON migration_jobs(migration_project_id);
CREATE INDEX idx_migration_jobs_phase_id ON migration_jobs(migration_phase_id);
CREATE INDEX idx_migration_jobs_status ON migration_jobs(status);
CREATE INDEX idx_migration_jobs_depends_on ON migration_jobs(depends_on_job_id);
CREATE INDEX idx_migration_logs_job_id ON migration_logs(migration_job_id);
