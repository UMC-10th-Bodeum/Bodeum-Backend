package com.bodeum.domain.ai.model.rag;

import com.bodeum.domain.ai.enums.AiResponseSourceType;

public record AiSourceKey(
        AiResponseSourceType sourceType,
        Long sourceId
) {
}
