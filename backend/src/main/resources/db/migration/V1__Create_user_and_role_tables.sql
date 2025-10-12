-- V1: Create user and role tables
-- Users, Roles, and Permissions for authentication and authorization

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMP,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP,
    password_reset_token VARCHAR(255),
    password_reset_token_expiry TIMESTAMP,
    email_verification_token VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_enabled ON users(enabled);
CREATE INDEX idx_roles_name ON roles(name);
CREATE INDEX idx_permissions_resource_action ON permissions(resource, action);

-- Insert default roles
INSERT INTO roles (name, description) VALUES
    ('ROLE_ADMIN', 'System Administrator with full access'),
    ('ROLE_DATA_STEWARD', 'Data Steward for data quality and governance'),
    ('ROLE_MIGRATION_SPECIALIST', 'Migration Specialist for data migrations'),
    ('ROLE_BUSINESS_USER', 'Business User with read-only access'),
    ('ROLE_COMPLIANCE_OFFICER', 'Compliance Officer for GDPR/CCPA compliance'),
    ('ROLE_AUDITOR', 'Auditor with audit log access');

-- Insert default permissions
INSERT INTO permissions (name, resource, action, description) VALUES
    ('USER_READ', 'USER', 'READ', 'Read user information'),
    ('USER_WRITE', 'USER', 'WRITE', 'Create and update users'),
    ('USER_DELETE', 'USER', 'DELETE', 'Delete users'),
    ('EXTRACTION_READ', 'EXTRACTION', 'READ', 'Read extraction jobs'),
    ('EXTRACTION_WRITE', 'EXTRACTION', 'WRITE', 'Create and update extraction jobs'),
    ('EXTRACTION_EXECUTE', 'EXTRACTION', 'EXECUTE', 'Execute extraction jobs'),
    ('MIGRATION_READ', 'MIGRATION', 'READ', 'Read migration projects'),
    ('MIGRATION_WRITE', 'MIGRATION', 'WRITE', 'Create and update migration projects'),
    ('MIGRATION_EXECUTE', 'MIGRATION', 'EXECUTE', 'Execute migration jobs'),
    ('DATA_QUALITY_READ', 'DATA_QUALITY', 'READ', 'Read data quality rules'),
    ('DATA_QUALITY_WRITE', 'DATA_QUALITY', 'WRITE', 'Create and update data quality rules'),
    ('COMPLIANCE_READ', 'COMPLIANCE', 'READ', 'Read compliance data'),
    ('COMPLIANCE_WRITE', 'COMPLIANCE', 'WRITE', 'Manage compliance policies'),
    ('AUDIT_READ', 'AUDIT', 'READ', 'Read audit logs'),
    ('SYSTEM_ADMIN', 'SYSTEM', 'ADMIN', 'Full system administration');

-- Assign permissions to admin role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ROLE_ADMIN';
