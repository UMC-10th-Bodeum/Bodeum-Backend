package com.bodeum.domain.ai.dto.response;

import java.time.Instant;

public record AiChatRoomResponse(
        Long aiChatRoomId,
        Instant createdAt,
        boolean showGuideModal,
        boolean hasTodayMessages,
        boolean hasPreviousMessages
) {

    public static AiChatRoomResponse of(
            Long aiChatRoomId,
            Instant createdAt,
            boolean showGuideModal,
            boolean hasTodayMessages,
            boolean hasPreviousMessages
    ) {
        return new AiChatRoomResponse(
                aiChatRoomId,
                createdAt,
                showGuideModal,
                hasTodayMessages,
                hasPreviousMessages
        );
    }
}
