package com.bodeum.domain.ai.dto.response;

import com.bodeum.domain.ai.enums.SenderType;

import java.time.Instant;
import java.util.List;

public record AiTodayMessageResponse(
        List<Message> messages
) {

    public static AiTodayMessageResponse of(
            List<Message> messages
    ) {
        return new AiTodayMessageResponse(messages);
    }

    public record Message(
            Long aiMessageId,
            SenderType senderType,
            String content,
            Instant createdAt,
            List<Source> sources,
            String warning
    ) {

        public static Message of(
                Long aiMessageId,
                SenderType senderType,
                String content,
                Instant createdAt,
                List<Source> sources,
                String warning
        ) {
            return new Message(
                    aiMessageId,
                    senderType,
                    content,
                    createdAt,
                    sources,
                    warning
            );
        }
    }

    public record Source(
            String sourceTitle,
            String sourceUrl,
            Instant updatedAt
    ) {

        public static Source of(
                String sourceTitle,
                String sourceUrl,
                Instant updatedAt
        ) {
            return new Source(
                    sourceTitle,
                    sourceUrl,
                    updatedAt
            );
        }
    }
}
