package com.bodeum.domain.ai.model;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import java.time.Instant;

public record AiReferenceDocument(
        String documentKey,
        String content,
        AiResponseSourceType sourceType,
        Long sourceId,
        String title,
        String url,
        Instant updatedAt
) {
}
