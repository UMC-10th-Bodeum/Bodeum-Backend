package com.bodeum.domain.ai.model.rag;

import com.bodeum.domain.ai.enums.AiQuestionIntent;
import java.util.List;

public record AiQuestionAnalysis(
        AiQuestionIntent intent,
        List<String> retrievalQueries
) {

    public AiQuestionAnalysis {
        intent = intent == null ? AiQuestionIntent.NONE : intent;
        retrievalQueries = retrievalQueries == null
                ? List.of()
                : retrievalQueries.stream()
                        .filter(query -> query != null && !query.isBlank())
                        .map(String::trim)
                        .distinct()
                        .limit(3)
                        .toList();
    }

    public static AiQuestionAnalysis fallback() {
        return new AiQuestionAnalysis(AiQuestionIntent.NONE, List.of());
    }
}
