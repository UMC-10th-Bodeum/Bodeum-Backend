package com.bodeum.domain.ai.model.answer;

import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import java.util.List;

public record AiStarterQuestionAnswer(
        AiAnswerStatus answerStatus,
        String content,
        List<AiReferenceDocument> sources
) {

    public AiStarterQuestionAnswer {
        sources = List.copyOf(sources);
    }

    public static AiStarterQuestionAnswer answered(
            String content,
            List<AiReferenceDocument> sources
    ) {
        return new AiStarterQuestionAnswer(AiAnswerStatus.ANSWERED, content, sources);
    }

    public static AiStarterQuestionAnswer regionRequired(String content) {
        return new AiStarterQuestionAnswer(
                AiAnswerStatus.REGION_REQUIRED,
                content,
                List.of()
        );
    }

    public static AiStarterQuestionAnswer noEvidence() {
        return new AiStarterQuestionAnswer(
                AiAnswerStatus.NO_EVIDENCE,
                null,
                List.of()
        );
    }

    public boolean hasEvidence() {
        return answerStatus == AiAnswerStatus.ANSWERED
                && content != null
                && !content.isBlank()
                && !sources.isEmpty();
    }

    public boolean isRegionRequired() {
        return answerStatus == AiAnswerStatus.REGION_REQUIRED;
    }
}
