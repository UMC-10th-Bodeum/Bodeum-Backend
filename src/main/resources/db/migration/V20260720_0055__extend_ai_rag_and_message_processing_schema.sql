-- AI RAG 및 메시지 처리 상태를 현재 도메인 모델에 맞게 확장한다.
-- 1) ai_source_review: INFO/NEWS/SITE 출처의 검토 상태를 관리한다.
-- 2) ai_external_source/document: 등록 외부 출처와 실제 확보·인용 문서를 관리한다.
-- 3) ai_response_source: SITE 출처 및 수정일이 없는 외부 출처를 허용한다.
-- 4) ai_message: 긴 AI 답변 저장과 사용자 질문의 AI 응답 처리 상태를 지원한다.

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

CREATE TABLE ai_external_document (
    ai_external_document_id BIGINT NOT NULL AUTO_INCREMENT,
    ai_external_source_id BIGINT NOT NULL,
    title VARCHAR(300) NOT NULL,
    source_url VARCHAR(1000) NOT NULL,
    source_url_hash VARCHAR(64) NOT NULL,
    source_updated_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (ai_external_document_id),
    CONSTRAINT uk_ai_external_document_url_hash UNIQUE (source_url_hash),
    CONSTRAINT fk_ai_external_document_source
        FOREIGN KEY (ai_external_source_id)
        REFERENCES ai_external_source (ai_external_source_id)
);

CREATE INDEX idx_ai_external_document_source
    ON ai_external_document (ai_external_source_id);

ALTER TABLE ai_response_source
    MODIFY COLUMN source_type VARCHAR(20) NOT NULL,
    MODIFY COLUMN source_updated_at DATETIME(6) NULL;

ALTER TABLE ai_message
    MODIFY COLUMN content TEXT NOT NULL,
    ADD COLUMN ai_response_status VARCHAR(20) NULL AFTER is_warning;
