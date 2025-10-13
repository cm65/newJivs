-- V102: Make document_id column nullable
-- The document_id VARCHAR column from V7 is not being used by the new Document entity
-- which uses the BIGINT id as primary key. Making document_id nullable to avoid constraint violations.

ALTER TABLE documents
ALTER COLUMN document_id DROP NOT NULL;
