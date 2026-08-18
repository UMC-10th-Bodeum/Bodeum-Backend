CREATE INDEX idx_ai_message_chat_created_id
    ON ai_message (ai_chat_room_id, created_at, ai_message_id);

CREATE INDEX idx_ai_message_chat_root_sender_created_id
    ON ai_message (
        ai_chat_room_id,
        context_root_message_id,
        sender_type,
        created_at,
        ai_message_id
    );
