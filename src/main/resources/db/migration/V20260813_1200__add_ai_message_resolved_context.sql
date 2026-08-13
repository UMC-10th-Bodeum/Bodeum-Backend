ALTER TABLE ai_message
    ADD COLUMN resolved_context JSON NULL
        COMMENT '후속 질문 복원을 위한 구조화된 검색 문맥'
        AFTER resolved_question;
