package com.bodeum.domain.ai.model;

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
