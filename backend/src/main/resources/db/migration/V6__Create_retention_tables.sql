-- V6: Create retention management tables
-- Retention policies and lifecycle management

CREATE TABLE retention_policies (
    id BIGSERIAL PRIMARY KEY,
    policy_name VARCHAR(200) NOT NULL,
    policy_code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    business_object_id BIGINT,
    retention_period_days INTEGER NOT NULL,
    retention_trigger VARCHAR(50) NOT NULL, -- TIME_BASED, EVENT_BASED, CUSTOM
    trigger_field VARCHAR(100),
    trigger_event VARCHAR(100),
    grace_period_days INTEGER DEFAULT 0,
    deletion_method VARCHAR(50) NOT NULL DEFAULT 'SOFT_DELETE', -- SOFT_DELETE, HARD_DELETE, ARCHIVE
    archive_location VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    legal_basis VARCHAR(100),
    regulatory_requirement VARCHAR(100),
    approval_required BOOLEAN NOT NULL DEFAULT FALSE,
    approved_by VARCHAR(50),
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    FOREIGN KEY (business_object_id) REFERENCES business_object_definitions(id) ON DELETE SET NULL
);

CREATE TABLE retention_schedules (
    id BIGSERIAL PRIMARY KEY,
    schedule_id VARCHAR(100) NOT NULL UNIQUE,
    policy_id BIGINT NOT NULL,
    schedule_type VARCHAR(50) NOT NULL, -- DAILY, WEEKLY, MONTHLY, ON_DEMAND
    cron_expression VARCHAR(100),
    next_run_time TIMESTAMP,
    last_run_time TIMESTAMP,
    last_run_status VARCHAR(50),
    last_run_records_processed BIGINT,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (policy_id) REFERENCES retention_policies(id) ON DELETE CASCADE
);

CREATE TABLE data_lifecycle_records (
    id BIGSERIAL PRIMARY KEY,
    policy_id BIGINT NOT NULL,
    record_id VARCHAR(255) NOT NULL,
    business_object_type VARCHAR(100) NOT NULL,
    record_created_date DATE,
    expiry_date DATE NOT NULL,
    lifecycle_status VARCHAR(50) NOT NULL, -- ACTIVE, EXPIRING_SOON, EXPIRED, ARCHIVED, DELETED, ON_HOLD
    deletion_scheduled_date DATE,
    actual_deletion_date DATE,
    legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    legal_hold_reason TEXT,
    legal_hold_start_date DATE,
    legal_hold_end_date DATE,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (policy_id) REFERENCES retention_policies(id) ON DELETE RESTRICT
);

CREATE TABLE retention_audit_trail (
    id BIGSERIAL PRIMARY KEY,
    policy_id BIGINT NOT NULL,
    record_id VARCHAR(255) NOT NULL,
    action VARCHAR(50) NOT NULL, -- ARCHIVED, DELETED, HOLD_APPLIED, HOLD_RELEASED, RETENTION_EXTENDED
    action_reason TEXT,
    performed_by VARCHAR(50) NOT NULL,
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    previous_state JSONB,
    new_state JSONB,
    FOREIGN KEY (policy_id) REFERENCES retention_policies(id) ON DELETE RESTRICT
);

-- Indexes
CREATE INDEX idx_retention_policies_code ON retention_policies(policy_code);
CREATE INDEX idx_retention_policies_active ON retention_policies(is_active);
CREATE INDEX idx_retention_schedules_policy_id ON retention_schedules(policy_id);
CREATE INDEX idx_retention_schedules_next_run ON retention_schedules(next_run_time);
CREATE INDEX idx_lifecycle_records_policy_id ON data_lifecycle_records(policy_id);
CREATE INDEX idx_lifecycle_records_status ON data_lifecycle_records(lifecycle_status);
CREATE INDEX idx_lifecycle_records_expiry_date ON data_lifecycle_records(expiry_date);
CREATE INDEX idx_lifecycle_records_legal_hold ON data_lifecycle_records(legal_hold);
CREATE INDEX idx_retention_audit_policy_id ON retention_audit_trail(policy_id);
CREATE INDEX idx_retention_audit_performed_at ON retention_audit_trail(performed_at);
