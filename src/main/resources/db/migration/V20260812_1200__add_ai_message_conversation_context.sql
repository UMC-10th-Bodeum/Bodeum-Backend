ALTER TABLE ai_message
    ADD COLUMN resolved_question TEXT NULL,
    ADD COLUMN context_parent_message_id BIGINT NULL,
    ADD COLUMN context_root_message_id BIGINT NULL;

CREATE INDEX idx_ai_message_context_parent
    ON ai_message (context_parent_message_id);

CREATE INDEX idx_ai_message_context_root
    ON ai_message (context_root_message_id);
