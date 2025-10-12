-- V5: Create data quality tables
-- Data quality rules, checks, and results

CREATE TABLE data_quality_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(200) NOT NULL,
    rule_code VARCHAR(50) NOT NULL UNIQUE,
    rule_type VARCHAR(50) NOT NULL, -- COMPLETENESS, ACCURACY, CONSISTENCY, VALIDITY, UNIQUENESS, TIMELINESS
    description TEXT,
    business_object_id BIGINT,
    field_name VARCHAR(100),
    rule_expression TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, CRITICAL
    threshold_value DECIMAL(10, 2),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    validation_query TEXT,
    error_message_template VARCHAR(500),
    remediation_guidance TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    FOREIGN KEY (business_object_id) REFERENCES business_object_definitions(id) ON DELETE SET NULL
);

CREATE TABLE data_quality_checks (
    id BIGSERIAL PRIMARY KEY,
    check_id VARCHAR(100) NOT NULL UNIQUE,
    rule_id BIGINT NOT NULL,
    target_table VARCHAR(100),
    target_schema VARCHAR(100),
    check_status VARCHAR(50) NOT NULL, -- PENDING, RUNNING, COMPLETED, FAILED
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    records_checked BIGINT DEFAULT 0,
    records_passed BIGINT DEFAULT 0,
    records_failed BIGINT DEFAULT 0,
    quality_score DECIMAL(5, 2),
    check_params JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    triggered_by VARCHAR(50),
    FOREIGN KEY (rule_id) REFERENCES data_quality_rules(id) ON DELETE RESTRICT
);

CREATE TABLE data_quality_results (
    id BIGSERIAL PRIMARY KEY,
    check_id BIGINT NOT NULL,
    record_id VARCHAR(255),
    field_name VARCHAR(100),
    field_value TEXT,
    expected_value TEXT,
    error_type VARCHAR(50),
    error_description TEXT,
    severity VARCHAR(20),
    is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(50),
    resolution_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (check_id) REFERENCES data_quality_checks(id) ON DELETE CASCADE
);

CREATE TABLE duplicate_records (
    id BIGSERIAL PRIMARY KEY,
    business_object_id BIGINT NOT NULL,
    master_record_id VARCHAR(255),
    duplicate_record_id VARCHAR(255),
    similarity_score DECIMAL(5, 4),
    matching_fields JSONB,
    match_algorithm VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, CONFIRMED, MERGED, IGNORED
    merge_strategy VARCHAR(50),
    merged_at TIMESTAMP,
    merged_by VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (business_object_id) REFERENCES business_object_definitions(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_data_quality_rules_code ON data_quality_rules(rule_code);
CREATE INDEX idx_data_quality_rules_type ON data_quality_rules(rule_type);
CREATE INDEX idx_data_quality_rules_active ON data_quality_rules(is_active);
CREATE INDEX idx_data_quality_checks_rule_id ON data_quality_checks(rule_id);
CREATE INDEX idx_data_quality_checks_status ON data_quality_checks(check_status);
CREATE INDEX idx_data_quality_results_check_id ON data_quality_results(check_id);
CREATE INDEX idx_data_quality_results_resolved ON data_quality_results(is_resolved);
CREATE INDEX idx_duplicate_records_object_id ON duplicate_records(business_object_id);
CREATE INDEX idx_duplicate_records_status ON duplicate_records(status);
