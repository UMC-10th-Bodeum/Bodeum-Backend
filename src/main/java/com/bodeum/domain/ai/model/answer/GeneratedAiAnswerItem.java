package com.bodeum.domain.ai.model.answer;

public record GeneratedAiAnswerItem(String name, String documentKey) {

    public GeneratedAiAnswerItem {
        name = name == null ? null : name.trim();
        documentKey = documentKey == null ? null : documentKey.trim();
    }
}
