package com.bodeum.domain.ai.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;

public enum AiStarterQuestionType {

    WELFARE_SITES(
            true,
            "참고하면 좋을 복지사이트 알려줘",
            "복지사이트 알려줘",
            "복지사이트를 알려줘",
            "복지 사이트 추천해줘",
            "복지사이트 추천해줘"
    ),
    LOCAL_REHAB_CENTERS(
            true,
            "우리 동네 재활센터 추천해줘",
            "우리 동네 재활센터를 추천해줘",
            "우리 지역 재활센터 알려줘",
            "우리 지역 재활센터를 알려줘"
    ),
    CHILD_MEDICAL_SUPPORT(
            true,
            "장애아동 의료비 지원이 궁금해",
            "장애아동 의료비 지원 알려줘",
            "장애아동 의료비 지원을 알려줘",
            "장애아동 병원비 지원 알려줘",
            "장애아동 병원비 지원을 알려줘"
    ),
    DIAGNOSIS_FIRST_STEPS(
            true,
            "장애 진단 후 첫 번째로 해야 할 일",
            "장애 진단 후 첫 번째로 해야 할 일이 뭐야",
            "장애 진단 후 첫 번째로 해야 할 일 알려줘",
            "장애 진단 후 첫 번째로 해야 할 일을 알려줘",
            "장애 진단 후 뭘 먼저 해야 해",
            "장애 진단 후 무엇부터 해야 해"
    ),
    VOUCHER_APPLICATION(
            true,
            "바우처 신청 방법 알려줘",
            "바우처 신청 방법을 알려줘",
            "바우처 어떻게 신청해",
            "발달재활서비스 바우처 신청 방법 알려줘"
    ),
    AUTISM_INFO_SITES(
            false,
            "자폐스펙트럼에 관한 정보를 얻을 수 있는 사이트는?",
            "자폐스펙트럼 정보 사이트 알려줘",
            "자폐 관련 공식 사이트 알려줘",
            "자폐 정보를 어디서 확인할 수 있나요"
    );

    private final boolean suggestedQuestion;
    private final String content;
    private final List<String> acceptedQuestions;

    AiStarterQuestionType(
            boolean suggestedQuestion,
            String content,
            String... aliases
    ) {
        this.suggestedQuestion = suggestedQuestion;
        this.content = content;
        this.acceptedQuestions = concat(
                        Stream.of(content),
                        Arrays.stream(aliases)
                )
                .map(AiStarterQuestionType::normalize)
                .distinct()
                .toList();
    }

    public String getContent() {
        return content;
    }

    public boolean isSuggestedQuestion() {
        return suggestedQuestion;
    }

    public boolean matches(String question) {
        return acceptedQuestions.contains(normalize(question));
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
