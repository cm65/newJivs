-- V9: Create audit logging tables
-- Comprehensive audit trail for all system activities

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    audit_id VARCHAR(100) NOT NULL UNIQUE,
    event_type VARCHAR(50) NOT NULL, -- USER_LOGIN, USER_LOGOUT, DATA_ACCESS, DATA_MODIFICATION, SYSTEM_CHANGE
    event_category VARCHAR(50) NOT NULL, -- SECURITY, DATA, SYSTEM, COMPLIANCE
    event_action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(255),
    user_id BIGINT,
    username VARCHAR(50),
    session_id VARCHAR(100),
    ip_address VARCHAR(45),
    user_agent TEXT,
    request_method VARCHAR(10),
    request_url VARCHAR(500),
    request_params JSONB,
    old_values JSONB,
    new_values JSONB,
    event_result VARCHAR(20) NOT NULL, -- SUCCESS, FAILURE, PARTIAL
    error_message TEXT,
    additional_details JSONB,
    event_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE audit_data_access (
    id BIGSERIAL PRIMARY KEY,
    audit_log_id BIGINT NOT NULL,
    business_object_type VARCHAR(100) NOT NULL,
    record_id VARCHAR(255) NOT NULL,
    field_name VARCHAR(100),
    access_type VARCHAR(20) NOT NULL, -- READ, WRITE, DELETE
    field_was_encrypted BOOLEAN DEFAULT FALSE,
    data_classification VARCHAR(20), -- PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED
    accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (audit_log_id) REFERENCES audit_logs(id) ON DELETE CASCADE
);

CREATE TABLE system_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL UNIQUE,
    event_type VARCHAR(50) NOT NULL, -- SERVICE_START, SERVICE_STOP, CONFIG_CHANGE, BACKUP, MIGRATION, DEPLOYMENT
    component_name VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL, -- INFO, WARNING, ERROR, CRITICAL
    event_message TEXT NOT NULL,
    event_details JSONB,
    hostname VARCHAR(255),
    process_id INTEGER,
    triggered_by VARCHAR(50),
    event_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE performance_metrics (
    id BIGSERIAL PRIMARY KEY,
    metric_name VARCHAR(100) NOT NULL,
    metric_type VARCHAR(50) NOT NULL, -- COUNTER, GAUGE, HISTOGRAM, SUMMARY
    metric_value DECIMAL(15, 4) NOT NULL,
    metric_unit VARCHAR(20),
    component_name VARCHAR(100),
    tags JSONB,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_audit_logs_audit_id ON audit_logs(audit_id);
CREATE INDEX idx_audit_logs_event_type ON audit_logs(event_type);
CREATE INDEX idx_audit_logs_event_category ON audit_logs(event_category);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_username ON audit_logs(username);
CREATE INDEX idx_audit_logs_resource_type_id ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(event_timestamp);
CREATE INDEX idx_audit_data_access_log_id ON audit_data_access(audit_log_id);
CREATE INDEX idx_audit_data_access_object_type ON audit_data_access(business_object_type);
CREATE INDEX idx_audit_data_access_timestamp ON audit_data_access(accessed_at);
CREATE INDEX idx_system_events_event_id ON system_events(event_id);
CREATE INDEX idx_system_events_type ON system_events(event_type);
CREATE INDEX idx_system_events_severity ON system_events(severity);
CREATE INDEX idx_system_events_timestamp ON system_events(event_timestamp);
CREATE INDEX idx_performance_metrics_name ON performance_metrics(metric_name);
CREATE INDEX idx_performance_metrics_timestamp ON performance_metrics(recorded_at);

-- Create partitioning for audit logs (by month) - Optional but recommended for large datasets
-- This is a comment showing how to partition - actual implementation may vary based on PostgreSQL version
-- ALTER TABLE audit_logs PARTITION BY RANGE (event_timestamp);
