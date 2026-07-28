package com.bodeum.domain.ai.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;

public enum AiStarterQuestionType {

    WELFARE_SITES("참고하면 좋을 복지사이트 알려줘"),
    LOCAL_REHAB_CENTERS("우리 동네 재활센터 추천해줘"),
    CHILD_MEDICAL_SUPPORT("장애아동 의료비 지원이 궁금해"),
    DIAGNOSIS_FIRST_STEPS("장애 진단 후 첫 번째로 해야 할 일"),
    VOUCHER_APPLICATION("바우처 신청 방법 알려줘");

    private final String content;
    private final List<String> acceptedQuestions;

    AiStarterQuestionType(String content, String... aliases) {
        this.content = content;
        this.acceptedQuestions = concat(
                        Stream.of(content),
                        Arrays.stream(aliases)
                )
                .toList();
    }

    public String getContent() {
        return content;
    }

    public boolean matches(String question) {
        String normalizedQuestion = normalize(question);
        return acceptedQuestions.stream()
                .map(AiStarterQuestionType::normalize)
                .anyMatch(normalizedQuestion::equals);
    }

    public static Optional<AiStarterQuestionType> fromQuestion(String question) {
        return Arrays.stream(values())
                .filter(type -> type.matches(question))
                .findFirst();
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim()
                .replaceFirst("[.!?~]+$", "")
                .replaceAll("\\s+", "");
        return normalized
                .replaceFirst("알려주세요$", "알려줘")
                .replaceFirst("추천해주세요$", "추천해줘")
                .replaceFirst("궁금합니다$", "궁금해")
                .replaceFirst("궁금해요$", "궁금해");
    }
}
