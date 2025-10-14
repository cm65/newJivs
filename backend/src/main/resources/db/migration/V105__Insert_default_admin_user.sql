-- V105: Insert default admin user
-- Create a default admin user for initial login

-- Insert default admin user
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
);

-- Assign ROLE_ADMIN to the admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN';
