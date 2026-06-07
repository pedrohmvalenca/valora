ALTER TABLE submissions
    ADD COLUMN proof_data BYTEA,
    ADD COLUMN proof_content_type VARCHAR(100);
