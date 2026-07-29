ALTER TABLE auth_refresh_token_sessions
    ADD COLUMN family_id VARCHAR(36) NULL,
    ADD COLUMN consumed_at TIMESTAMP(6) NULL;

UPDATE auth_refresh_token_sessions
SET family_id = UUID()
WHERE family_id IS NULL;

ALTER TABLE auth_refresh_token_sessions
    MODIFY COLUMN family_id VARCHAR(36) NOT NULL;

CREATE INDEX idx_auth_refresh_token_sessions_family_id
    ON auth_refresh_token_sessions (family_id);
