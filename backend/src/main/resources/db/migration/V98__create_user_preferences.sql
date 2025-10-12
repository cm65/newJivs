-- Create user_preferences table for storing user-specific settings
-- Part of Sprint 2 - Workflow 6: Dark Mode Implementation

CREATE TABLE user_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    theme VARCHAR(20) DEFAULT 'light' NOT NULL,
    language VARCHAR(10) DEFAULT 'en' NOT NULL,
    notifications_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    email_notifications BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT uk_user_preferences_user_id UNIQUE (user_id),
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_theme CHECK (theme IN ('light', 'dark', 'auto'))
);

-- Indexes for performance
CREATE INDEX idx_user_preferences_user_id ON user_preferences(user_id);
CREATE INDEX idx_user_preferences_theme ON user_preferences(theme);

-- Comments
COMMENT ON TABLE user_preferences IS 'Stores user-specific preferences including theme, language, and notification settings';
COMMENT ON COLUMN user_preferences.theme IS 'UI theme preference: light, dark, or auto';
COMMENT ON COLUMN user_preferences.language IS 'Preferred language code (ISO 639-1)';
COMMENT ON COLUMN user_preferences.notifications_enabled IS 'Master toggle for all notifications';
COMMENT ON COLUMN user_preferences.email_notifications IS 'Toggle for email notifications';
