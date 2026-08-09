package com.bodeum.domain.ai.model.rag;

import com.bodeum.domain.ai.enums.AiQuestionIntent;
import java.util.List;
import java.util.stream.Stream;

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

    public static AiQuestionAnalysis fallback(String question) {
        return forQuestion(question, AiQuestionIntent.NONE, List.of());
    }

    public static AiQuestionAnalysis forQuestion(
            String question,
            AiQuestionIntent intent,
            List<String> expandedQueries
    ) {
        AiQuestionIntent resolvedIntent = intent == null
                ? AiQuestionIntent.NONE
                : intent;
        if (resolvedIntent != AiQuestionIntent.NONE) {
            return new AiQuestionAnalysis(resolvedIntent, List.of());
        }

        Stream<String> expansions = expandedQueries == null
                ? Stream.empty()
                : expandedQueries.stream();
        List<String> retrievalQueries = Stream.concat(
                        Stream.of(question),
                        expansions
                )
                .filter(query -> query != null && !query.isBlank())
                .map(String::trim)
                .distinct()
                .limit(3)
                .toList();
        return new AiQuestionAnalysis(resolvedIntent, retrievalQueries);
    }
}
