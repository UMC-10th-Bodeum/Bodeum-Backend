package com.bodeum.domain.ai.dto.response;

import java.time.Instant;
import java.util.List;

public record AiTodayMessageResponse(
        List<AiMessageResponse> messages,
        Cursor nextCursor,
        boolean hasNext
) {

    public static AiTodayMessageResponse of(
            List<AiMessageResponse> messages,
            Cursor nextCursor,
            boolean hasNext
    ) {
        return new AiTodayMessageResponse(
                messages == null ? List.of() : List.copyOf(messages),
                nextCursor,
                hasNext
        );
    }

    public record Cursor(
            Long id,
            Instant createdAt
    ) {
    }
}
