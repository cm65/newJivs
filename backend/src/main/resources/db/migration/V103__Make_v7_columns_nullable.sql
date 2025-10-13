-- V103: Make V7 document columns nullable
-- The new Document entity uses different column names added in V101
-- Make the old V7 columns nullable to avoid NOT NULL constraint violations

ALTER TABLE documents
ALTER COLUMN document_name DROP NOT NULL,
ALTER COLUMN original_filename DROP NOT NULL,
ALTER COLUMN file_extension DROP NOT NULL,
ALTER COLUMN mime_type DROP NOT NULL,
ALTER COLUMN file_size_bytes DROP NOT NULL,
ALTER COLUMN storage_location DROP NOT NULL;
