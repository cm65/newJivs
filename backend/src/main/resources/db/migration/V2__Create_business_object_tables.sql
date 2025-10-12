-- V2: Create business object tables
-- Business Object Framework for data modeling

CREATE TABLE business_object_definitions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    technical_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    object_type VARCHAR(50) NOT NULL, -- SAP, CUSTOM, STANDARD
    source_system VARCHAR(100),
    table_name VARCHAR(100),
    category VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

CREATE TABLE business_object_fields (
    id BIGSERIAL PRIMARY KEY,
    business_object_id BIGINT NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    technical_name VARCHAR(100) NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    length INTEGER,
    precision_value INTEGER,
    scale_value INTEGER,
    is_key BOOLEAN NOT NULL DEFAULT FALSE,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    is_encrypted BOOLEAN NOT NULL DEFAULT FALSE,
    is_pii BOOLEAN NOT NULL DEFAULT FALSE,
    default_value VARCHAR(255),
    description TEXT,
    validation_rule TEXT,
    sequence_order INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (business_object_id) REFERENCES business_object_definitions(id) ON DELETE CASCADE
);

CREATE TABLE business_object_relationships (
    id BIGSERIAL PRIMARY KEY,
    source_object_id BIGINT NOT NULL,
    target_object_id BIGINT NOT NULL,
    relationship_type VARCHAR(50) NOT NULL, -- ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY
    relationship_name VARCHAR(100),
    source_field VARCHAR(100),
    target_field VARCHAR(100),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (source_object_id) REFERENCES business_object_definitions(id) ON DELETE CASCADE,
    FOREIGN KEY (target_object_id) REFERENCES business_object_definitions(id) ON DELETE CASCADE
);

CREATE TABLE business_object_versions (
    id BIGSERIAL PRIMARY KEY,
    business_object_id BIGINT NOT NULL,
    version_number INTEGER NOT NULL,
    definition_json JSONB NOT NULL,
    change_description TEXT,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    FOREIGN KEY (business_object_id) REFERENCES business_object_definitions(id) ON DELETE CASCADE,
    UNIQUE (business_object_id, version_number)
);

-- Indexes
CREATE INDEX idx_business_objects_technical_name ON business_object_definitions(technical_name);
CREATE INDEX idx_business_objects_type ON business_object_definitions(object_type);
CREATE INDEX idx_business_objects_active ON business_object_definitions(is_active);
CREATE INDEX idx_business_object_fields_object_id ON business_object_fields(business_object_id);
CREATE INDEX idx_business_object_fields_technical_name ON business_object_fields(technical_name);
CREATE INDEX idx_business_object_relationships_source ON business_object_relationships(source_object_id);
CREATE INDEX idx_business_object_relationships_target ON business_object_relationships(target_object_id);
CREATE INDEX idx_business_object_versions_object_id ON business_object_versions(business_object_id);
