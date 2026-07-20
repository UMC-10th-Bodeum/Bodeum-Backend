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
        if (answerStatus == AiAnswerStatus.ANSWERED && sources.isEmpty()) {
            throw new IllegalArgumentException("ANSWERED message must have at least one source");
        }
        if (answerStatus == AiAnswerStatus.NO_EVIDENCE && !sources.isEmpty()) {
            throw new IllegalArgumentException("NO_EVIDENCE message must not have sources");
        }
    }

    public static AiMessageResponse answered(
            Long aiMessageId,
            SenderType senderType,
            String content,
            Instant createdAt,
            List<AiMessageSourceResponse> sources,
            AiMessageWarningResponse warning
    ) {
        return new AiMessageResponse(
                aiMessageId,
                senderType,
                AiAnswerStatus.ANSWERED,
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
