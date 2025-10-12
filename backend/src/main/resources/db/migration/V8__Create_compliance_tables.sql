-- V8: Create compliance management tables
-- GDPR, CCPA, and compliance management

CREATE TABLE compliance_policies (
    id BIGSERIAL PRIMARY KEY,
    policy_name VARCHAR(200) NOT NULL,
    policy_code VARCHAR(50) NOT NULL UNIQUE,
    policy_type VARCHAR(50) NOT NULL, -- GDPR, CCPA, HIPAA, CUSTOM
    description TEXT,
    jurisdiction VARCHAR(100),
    effective_date DATE NOT NULL,
    expiry_date DATE,
    policy_document_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

CREATE TABLE data_subject_requests (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(100) NOT NULL UNIQUE,
    request_type VARCHAR(50) NOT NULL, -- ACCESS, DELETION, RECTIFICATION, PORTABILITY, RESTRICTION, OBJECTION
    subject_email VARCHAR(255) NOT NULL,
    subject_name VARCHAR(200),
    subject_identifier VARCHAR(255),
    request_details TEXT,
    request_source VARCHAR(50), -- WEB, EMAIL, PHONE, LETTER
    status VARCHAR(50) NOT NULL, -- RECEIVED, UNDER_REVIEW, IN_PROGRESS, COMPLETED, REJECTED, CANCELLED
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    due_date DATE NOT NULL,
    completed_date DATE,
    verification_status VARCHAR(50), -- PENDING, VERIFIED, FAILED
    verification_method VARCHAR(50),
    verified_at TIMESTAMP,
    verified_by VARCHAR(50),
    assigned_to VARCHAR(50),
    response_message TEXT,
    internal_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE consent_records (
    id BIGSERIAL PRIMARY KEY,
    subject_identifier VARCHAR(255) NOT NULL,
    subject_email VARCHAR(255),
    consent_type VARCHAR(100) NOT NULL, -- MARKETING, DATA_PROCESSING, THIRD_PARTY_SHARING, PROFILING
    consent_purpose TEXT NOT NULL,
    consent_given BOOLEAN NOT NULL,
    consent_version VARCHAR(20),
    consent_text TEXT,
    consent_channel VARCHAR(50), -- WEB, MOBILE, EMAIL, PHONE
    ip_address VARCHAR(45),
    user_agent TEXT,
    consent_date TIMESTAMP NOT NULL,
    expiry_date TIMESTAMP,
    withdrawal_date TIMESTAMP,
    last_updated TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE privacy_impact_assessments (
    id BIGSERIAL PRIMARY KEY,
    assessment_id VARCHAR(100) NOT NULL UNIQUE,
    project_name VARCHAR(200) NOT NULL,
    description TEXT,
    assessment_type VARCHAR(50), -- DPIA, PIA, TIA
    data_processing_description TEXT,
    necessity_and_proportionality TEXT,
    risks_identified TEXT,
    risk_level VARCHAR(20), -- LOW, MEDIUM, HIGH, CRITICAL
    mitigation_measures TEXT,
    residual_risk VARCHAR(20),
    status VARCHAR(50) NOT NULL, -- DRAFT, UNDER_REVIEW, APPROVED, REJECTED
    conducted_by VARCHAR(50),
    reviewed_by VARCHAR(50),
    approved_by VARCHAR(50),
    approved_at TIMESTAMP,
    review_date DATE,
    next_review_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE data_breach_incidents (
    id BIGSERIAL PRIMARY KEY,
    incident_id VARCHAR(100) NOT NULL UNIQUE,
    incident_title VARCHAR(255) NOT NULL,
    incident_description TEXT NOT NULL,
    incident_type VARCHAR(50), -- UNAUTHORIZED_ACCESS, DATA_LOSS, RANSOMWARE, PHISHING, OTHER
    severity VARCHAR(20) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    affected_data_types TEXT,
    estimated_affected_records BIGINT,
    discovery_date TIMESTAMP NOT NULL,
    occurrence_date TIMESTAMP,
    breach_source VARCHAR(100),
    status VARCHAR(50) NOT NULL, -- DETECTED, INVESTIGATING, CONTAINED, RESOLVED, REPORTED
    containment_measures TEXT,
    remediation_actions TEXT,
    notification_required BOOLEAN NOT NULL DEFAULT FALSE,
    notification_sent BOOLEAN NOT NULL DEFAULT FALSE,
    notification_date DATE,
    regulatory_notification_required BOOLEAN NOT NULL DEFAULT FALSE,
    regulatory_notification_date DATE,
    reported_by VARCHAR(50),
    assigned_to VARCHAR(50),
    resolved_at TIMESTAMP,
    lessons_learned TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_compliance_policies_code ON compliance_policies(policy_code);
CREATE INDEX idx_compliance_policies_type ON compliance_policies(policy_type);
CREATE INDEX idx_data_subject_requests_request_id ON data_subject_requests(request_id);
CREATE INDEX idx_data_subject_requests_status ON data_subject_requests(status);
CREATE INDEX idx_data_subject_requests_due_date ON data_subject_requests(due_date);
CREATE INDEX idx_consent_records_subject ON consent_records(subject_identifier);
CREATE INDEX idx_consent_records_type ON consent_records(consent_type);
CREATE INDEX idx_consent_records_consent_date ON consent_records(consent_date);
CREATE INDEX idx_pia_assessment_id ON privacy_impact_assessments(assessment_id);
CREATE INDEX idx_pia_status ON privacy_impact_assessments(status);
CREATE INDEX idx_breach_incident_id ON data_breach_incidents(incident_id);
CREATE INDEX idx_breach_status ON data_breach_incidents(status);
CREATE INDEX idx_breach_severity ON data_breach_incidents(severity);
