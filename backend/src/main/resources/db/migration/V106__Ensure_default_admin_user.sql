-- V106: Ensure default admin user exists with ON CONFLICT handling
-- This migration ensures the admin user is created if it doesn't already exist

-- Insert or update default admin user
-- Password: 'password' (BCrypt hash with strength 10)
INSERT INTO users (username, email, password_hash, first_name, last_name, enabled, account_non_expired, account_non_locked, credentials_non_expired, email_verified, created_by)
VALUES (
    'admin',
    'admin@jivs.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- password: 'password'
    'System',
    'Administrator',
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    'SYSTEM'
)
ON CONFLICT (username) DO NOTHING;

-- Ensure ROLE_ADMIN is assigned to admin user (idempotent)
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;
