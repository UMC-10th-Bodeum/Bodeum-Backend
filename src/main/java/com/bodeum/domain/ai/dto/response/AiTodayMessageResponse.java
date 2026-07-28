package com.bodeum.domain.ai.dto.response;

import java.util.List;

public record AiTodayMessageResponse(
        List<AiMessageResponse> messages
) {

    public static AiTodayMessageResponse of(
            List<AiMessageResponse> messages
    ) {
        return new AiTodayMessageResponse(
                messages == null ? List.of() : List.copyOf(messages)
        );
    }
}
