-- USER 질문의 AI 처리 상태와 AI 답변 결과 상태를 명확하게 구분한다.
-- 1) ai_response_status를 ai_processing_status로 변경한다.
-- 2) 이력 조회에서도 동일한 답변 유형을 반환할 수 있도록 ai_answer_status를 추가한다.
-- 3) 기존 AI 메시지는 출처 존재 여부에 따라 ANSWERED 또는 NO_EVIDENCE로 보정한다.

ALTER TABLE ai_message
    RENAME COLUMN ai_response_status TO ai_processing_status,
    ADD COLUMN ai_answer_status VARCHAR(20) NULL AFTER ai_processing_status;

UPDATE ai_message message
SET message.ai_answer_status = CASE
    WHEN EXISTS (
        SELECT 1
        FROM ai_response_source source
        WHERE source.ai_message_id = message.ai_message_id
    ) THEN 'ANSWERED'
    ELSE 'NO_EVIDENCE'
END
WHERE message.sender_type = 'AI';
