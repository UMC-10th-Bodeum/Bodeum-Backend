package com.bodeum.domain.ai.model.answer;

import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import java.util.List;

public record ExternalAiAnswer(
        String answer,
        List<AiReferenceDocument> sources
) {

    public static ExternalAiAnswer empty() {
        return new ExternalAiAnswer(null, List.of());
    }

    public boolean hasEvidence() {
        return answer != null && !answer.isBlank() && sources != null && !sources.isEmpty();
    }
}
