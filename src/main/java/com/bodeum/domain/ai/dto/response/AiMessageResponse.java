package com.bodeum.domain.ai.dto.response;

import com.bodeum.domain.ai.enums.SenderType;
import java.time.Instant;
import java.util.List;

public record AiMessageResponse(
        Long aiMessageId,
        SenderType senderType,
        String content,
        Instant createdAt,
        List<AiMessageSourceResponse> sources,
        String warning,
        String disclaimer
) {

    public AiMessageResponse {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
