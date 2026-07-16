-- Create auth support tables required by JWT refresh-token rotation and OAuth state validation.
-- Apply before deploying code that runs with spring.jpa.hibernate.ddl-auto=validate.

CREATE TABLE IF NOT EXISTS auth_refresh_token_sessions (
    token_hash VARCHAR(64) NOT NULL,
    user_id    BIGINT      NOT NULL,
    expires_at TIMESTAMP   NOT NULL,
    PRIMARY KEY (token_hash),
    INDEX idx_auth_refresh_token_sessions_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS oauth_states (
    state      VARCHAR(64) NOT NULL,
    provider   VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP   NOT NULL,
    PRIMARY KEY (state)
);
