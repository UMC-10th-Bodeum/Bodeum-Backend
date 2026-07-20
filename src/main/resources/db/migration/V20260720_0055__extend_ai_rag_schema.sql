CREATE TABLE ai_source_review (
    ai_source_review_id BIGINT NOT NULL AUTO_INCREMENT,
    source_type VARCHAR(20) NOT NULL,
    source_id BIGINT NOT NULL,
    review_status VARCHAR(30) NOT NULL,
    review_note VARCHAR(500) NULL,
    reviewed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (ai_source_review_id),
    CONSTRAINT uk_ai_source_review_source UNIQUE (source_type, source_id)
);

CREATE TABLE ai_external_source (
    ai_external_source_id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    entry_url VARCHAR(1000) NULL,
    description TEXT NOT NULL,
    authority_level VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (ai_external_source_id),
    CONSTRAINT uk_ai_external_source_name_url UNIQUE (name, base_url)
);

CREATE TABLE ai_external_resource (
    ai_external_resource_id BIGINT NOT NULL AUTO_INCREMENT,
    ai_external_source_id BIGINT NOT NULL,
    title VARCHAR(300) NOT NULL,
    source_url VARCHAR(1000) NOT NULL,
    source_url_hash CHAR(64) NOT NULL,
    source_updated_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (ai_external_resource_id),
    CONSTRAINT uk_ai_external_resource_url_hash UNIQUE (source_url_hash),
    CONSTRAINT fk_ai_external_resource_source
        FOREIGN KEY (ai_external_source_id)
        REFERENCES ai_external_source (ai_external_source_id)
);

CREATE INDEX idx_ai_external_resource_source
    ON ai_external_resource (ai_external_source_id);

ALTER TABLE ai_response_source
    MODIFY COLUMN source_type VARCHAR(20) NOT NULL,
    MODIFY COLUMN source_updated_at DATETIME(6) NULL;

ALTER TABLE ai_message
    MODIFY COLUMN content TEXT NOT NULL;
