package com.bodeum.domain.ai.dto.response;

import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AiMessageResponse(
        Long aiMessageId,
        SenderType senderType,
        AiAnswerStatus answerStatus,
        String content,
        Instant createdAt,
        List<AiMessageSourceResponse> sources,
        AiMessageWarningResponse warning
) {

    public AiMessageResponse {
        Objects.requireNonNull(answerStatus, "answerStatus must not be null");
        sources = sources == null ? List.of() : List.copyOf(sources);
        if ((answerStatus == AiAnswerStatus.ANSWERED
                || answerStatus == AiAnswerStatus.LINK_GUIDANCE) && sources.isEmpty()) {
            throw new IllegalArgumentException(
                    "ANSWERED or LINK_GUIDANCE message must have at least one source");
        }
        if (answerStatus == AiAnswerStatus.NO_EVIDENCE && !sources.isEmpty()) {
            throw new IllegalArgumentException("NO_EVIDENCE message must not have sources");
        }
    }

    public static AiMessageResponse sourceBacked(
            Long aiMessageId,
            SenderType senderType,
            AiAnswerStatus answerStatus,
            String content,
            Instant createdAt,
            List<AiMessageSourceResponse> sources,
            AiMessageWarningResponse warning
    ) {
        if (answerStatus == AiAnswerStatus.NO_EVIDENCE) {
            throw new IllegalArgumentException(
                    "source-backed response cannot have NO_EVIDENCE status");
        }
        return new AiMessageResponse(
                aiMessageId,
                senderType,
                answerStatus,
                content,
                createdAt,
                sources,
                warning
        );
    }

    public static AiMessageResponse noEvidence(
            Long aiMessageId,
            SenderType senderType,
            String content,
            Instant createdAt
    ) {
        return new AiMessageResponse(
                aiMessageId,
                senderType,
                AiAnswerStatus.NO_EVIDENCE,
                content,
                createdAt,
                List.of(),
                null
        );
    }
}
