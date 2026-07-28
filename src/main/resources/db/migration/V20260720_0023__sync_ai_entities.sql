-- RDS의 ai_* 테이블을 현재 AI 도메인 엔티티 상태에 맞춘다.
-- 1) ai_chat_room: User 1:1 연관관계(user_id) 및 안내 확인 시각(last_guide_confirmed_at) 컬럼 반영
-- 2) ai_feedback_reason: @Table(uniqueConstraints=(ai_feedback_id, reason)) 반영
-- 3) ai_message: 경고 메시지 여부(is_warning) 컬럼 반영
-- (ai_feedback, ai_response_source는 엔티티와 이미 일치하여 변경 없음)

ALTER TABLE ai_chat_room
    ADD COLUMN user_id BIGINT NOT NULL AFTER created_at,
    ADD CONSTRAINT uk_ai_chat_room_user UNIQUE (user_id),
    ADD CONSTRAINT fk_ai_chat_room_user FOREIGN KEY (user_id) REFERENCES users (id),
    ADD COLUMN last_guide_confirmed_at DATETIME(6) NULL AFTER last_message_at;

ALTER TABLE ai_feedback_reason
    ADD CONSTRAINT uk_ai_feedback_reason_feedback_reason UNIQUE (ai_feedback_id, reason);

ALTER TABLE ai_message
    ADD COLUMN is_warning BIT NOT NULL AFTER content;