-- Create saved_views table for storing user-defined saved views (filters, sorting, columns)
CREATE TABLE saved_views (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    module VARCHAR(50) NOT NULL,
    view_name VARCHAR(100) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE NOT NULL,
    filters JSONB,
    sorting JSONB,
    visible_columns JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Foreign key to users table
    CONSTRAINT fk_saved_views_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- Unique constraint: user can't have duplicate view names per module
    CONSTRAINT uk_saved_views_user_module_name UNIQUE (user_id, module, view_name),

    -- Check constraint for valid module values
    CONSTRAINT chk_module CHECK (module IN ('extractions', 'migrations', 'data-quality', 'compliance'))
);

-- Create indexes for common queries
CREATE INDEX idx_saved_views_user_id ON saved_views(user_id);
CREATE INDEX idx_saved_views_module ON saved_views(module);
CREATE INDEX idx_saved_views_user_module ON saved_views(user_id, module);
CREATE INDEX idx_saved_views_is_default ON saved_views(is_default) WHERE is_default = TRUE;

-- Add comment
COMMENT ON TABLE saved_views IS 'Stores user-defined saved views with filters, sorting, and column preferences for different modules';
COMMENT ON COLUMN saved_views.module IS 'Module name: extractions, migrations, data-quality, compliance';
COMMENT ON COLUMN saved_views.is_default IS 'Whether this is the default view for the module';
COMMENT ON COLUMN saved_views.filters IS 'JSON object containing filter criteria';
COMMENT ON COLUMN saved_views.sorting IS 'JSON object containing sort field and direction';
COMMENT ON COLUMN saved_views.visible_columns IS 'JSON array of visible column names';
