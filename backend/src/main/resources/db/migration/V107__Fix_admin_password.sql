-- V107: Fix admin user password
-- Delete the admin user so DataInitializer can recreate it with correct password
DELETE FROM user_roles WHERE user_id = (SELECT id FROM users WHERE username = 'admin');
DELETE FROM users WHERE username = 'admin';
