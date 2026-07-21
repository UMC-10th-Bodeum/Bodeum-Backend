package com.bodeum.domain.ai.dto.response;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import java.time.Instant;

public record AiMessageSourceResponse(
        AiResponseSourceType sourceType,
        Long sourceId,
        String sourceTitle,
        String sourceUrl,
        Instant updatedAt
) {
}
