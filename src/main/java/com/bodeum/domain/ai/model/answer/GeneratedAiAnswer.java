package com.bodeum.domain.ai.model.answer;

import java.util.List;

public record GeneratedAiAnswer(
        String answer,
        List<String> citedDocumentKeys,
        List<GeneratedAiAnswerItem> answerItems
) {

    public GeneratedAiAnswer {
        citedDocumentKeys = citedDocumentKeys == null
                ? List.of()
                : List.copyOf(citedDocumentKeys);
        answerItems = answerItems == null ? List.of() : List.copyOf(answerItems);
    }

    public GeneratedAiAnswer(String answer, List<String> citedDocumentKeys) {
        this(answer, citedDocumentKeys, List.of());
    }
}
