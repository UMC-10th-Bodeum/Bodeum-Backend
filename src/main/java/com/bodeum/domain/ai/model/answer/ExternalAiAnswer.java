package com.bodeum.domain.ai.model.answer;

import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import java.util.List;
import java.util.Objects;

public record ExternalAiAnswer(
        String answer,
        List<AiReferenceDocument> sources,
        AiAnswerStatus answerStatus
) {

    public ExternalAiAnswer {
        Objects.requireNonNull(answerStatus, "answerStatus must not be null");
        sources = sources == null ? List.of() : List.copyOf(sources);
    }

    public ExternalAiAnswer(String answer, List<AiReferenceDocument> sources) {
        this(answer, sources, AiAnswerStatus.ANSWERED);
    }

    public static ExternalAiAnswer empty() {
        return new ExternalAiAnswer(null, List.of(), AiAnswerStatus.NO_EVIDENCE);
    }

    public static ExternalAiAnswer linkGuidance(
            String answer,
            List<AiReferenceDocument> sources
    ) {
        return new ExternalAiAnswer(answer, sources, AiAnswerStatus.LINK_GUIDANCE);
    }

    public boolean hasEvidence() {
        return answer != null && !answer.isBlank() && !sources.isEmpty();
    }
}
