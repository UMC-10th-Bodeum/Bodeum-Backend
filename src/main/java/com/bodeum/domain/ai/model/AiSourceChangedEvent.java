package com.bodeum.domain.ai.model;

import com.bodeum.domain.ai.enums.AiResponseSourceType;

public record AiSourceChangedEvent(
        AiResponseSourceType sourceType,
        Long sourceId,
        ChangeType changeType
) {

    public enum ChangeType {
        UPSERT,
        DELETE
    }
}
