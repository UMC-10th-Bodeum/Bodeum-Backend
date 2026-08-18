package com.bodeum.domain.ai.model.question;

import com.bodeum.domain.ai.util.AiTextNormalizer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * 채팅 시작 화면에 노출하거나 정확한 문장으로 인식할 질문 정의이다.
 */
public record AiStarterQuestion(
        AiCuratedAnswerType answerType,
        boolean visible,
        String content,
        List<String> acceptedQuestions
) {
    public AiStarterQuestion(
            AiCuratedAnswerType answerType,
            boolean visible,
            String content,
            String... aliases
    ) {
        this(
                answerType,
                visible,
                content,
                Stream.concat(Stream.of(content), Arrays.stream(aliases))
                        .map(AiStarterQuestion::normalize)
                        .distinct()
                        .toList()
        );
    }

    public boolean matches(String question) {
        return acceptedQuestions.contains(normalize(question));
    }

    private static String normalize(String value) {
        String normalized = AiTextNormalizer.removeWhitespace(
                AiTextNormalizer.normalizeQuestionSpacing(value));
        return normalized
                .replaceFirst("알려주세요$", "알려줘")
                .replaceFirst("추천해주세요$", "추천해줘")
                .replaceFirst("궁금합니다$", "궁금해")
                .replaceFirst("궁금해요$", "궁금해");
    }
}
