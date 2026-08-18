package com.bodeum.domain.ai.util;

public final class AiTextNormalizer {

    private AiTextNormalizer() {
    }

    public static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static String removeWhitespace(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", "");
    }

    public static String normalizeQuestionSpacing(String value) {
        return value == null
                ? ""
                : value.trim()
                        .replaceFirst("[.!?~]+$", "")
                        .trim()
                        .replaceAll("\\s+", " ");
    }
}
