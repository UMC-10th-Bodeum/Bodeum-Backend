ALTER TABLE guardian_point_history
    ADD COLUMN event_type VARCHAR(50) NULL AFTER point_value,
    ADD COLUMN reference_id BIGINT NULL AFTER event_type,
    ADD COLUMN actor_user_id BIGINT NULL AFTER reference_id;

ALTER TABLE guardian_point_history
    ADD CONSTRAINT uk_point_history_event_reference
        UNIQUE (event_type, reference_id, actor_user_id);
