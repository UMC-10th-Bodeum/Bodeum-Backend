package com.bodeum.domain.ai.infrastructure.support;

public final class AiPromptTemplate {

    private AiPromptTemplate() {
    }

    public static String replaceRequiredPlaceholder(
            String prompt,
            String placeholder,
            String value
    ) {
        if (prompt == null) {
            throw new IllegalStateException("AI prompt must not be null");
        }
        if (placeholder == null || placeholder.isBlank()) {
            throw new IllegalArgumentException("Prompt placeholder must not be blank");
        }
        if (!prompt.contains(placeholder)) {
            throw new IllegalStateException(
                    "Required AI prompt placeholder is missing: " + placeholder);
        }
        return prompt.replace(placeholder, value == null ? "" : value);
    }
}
