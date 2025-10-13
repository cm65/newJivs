-- V104: Make storage_path nullable
-- The storage_path is set after the first save (to get the generated ID)
-- so it needs to be nullable

ALTER TABLE documents
ALTER COLUMN storage_path DROP NOT NULL;
