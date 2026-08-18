package com.bodeum.domain.ai.model.question;

import java.util.List;
import java.util.Optional;

/**
 * 추천 질문칩 문구와 전용 답변의 연결을 관리한다.
 */
public final class AiStarterQuestionCatalog {

    private static final List<AiStarterQuestion> QUESTIONS = List.of(
            new AiStarterQuestion(
                    AiCuratedAnswerType.WELFARE_SITES, true,
                    "참고하면 좋을 복지사이트 알려줘",
                    "복지사이트 알려줘", "복지사이트를 알려줘",
                    "복지 사이트 추천해줘", "복지사이트 추천해줘"),
            new AiStarterQuestion(
                    AiCuratedAnswerType.LOCAL_REHAB_CENTERS, true,
                    "우리 동네 재활센터 추천해줘",
                    "우리 동네 재활센터를 추천해줘",
                    "우리 지역 재활센터 알려줘", "우리 지역 재활센터를 알려줘"),
            new AiStarterQuestion(
                    AiCuratedAnswerType.CHILD_MEDICAL_SUPPORT, true,
                    "장애아동 의료비 지원이 궁금해",
                    "장애아동 의료비 지원 알려줘", "장애아동 의료비 지원을 알려줘",
                    "장애아동 병원비 지원 알려줘", "장애아동 병원비 지원을 알려줘"),
            new AiStarterQuestion(
                    AiCuratedAnswerType.DIAGNOSIS_FIRST_STEPS, true,
                    "장애 진단 후 첫 번째로 해야 할 일",
                    "장애 진단 후 첫 번째로 해야 할 일이 뭐야",
                    "장애 진단 후 첫 번째로 해야 할 일 알려줘",
                    "장애 진단 후 첫 번째로 해야 할 일을 알려줘",
                    "장애 진단 후 뭘 먼저 해야 해", "장애 진단 후 무엇부터 해야 해"),
            new AiStarterQuestion(
                    AiCuratedAnswerType.VOUCHER_APPLICATION, true,
                    "바우처 신청 방법 알려줘",
                    "발달재활서비스 바우처 신청 방법 알려줘",
                    "발달재활서비스 바우처 신청 방법을 알려줘",
                    "발달재활 바우처 어떻게 신청해"),
            new AiStarterQuestion(
                    AiCuratedAnswerType.AUTISM_INFO_SITES, false,
                    "자폐스펙트럼에 관한 정보를 얻을 수 있는 사이트는?",
                    "자폐스펙트럼 정보 사이트 알려줘",
                    "자폐 관련 공식 사이트 알려줘",
                    "자폐 정보를 어디서 확인할 수 있나요")
    );

    private AiStarterQuestionCatalog() {
    }

    public static Optional<AiCuratedAnswerType> findAnswerType(String question) {
        return QUESTIONS.stream()
                .filter(candidate -> candidate.matches(question))
                .map(AiStarterQuestion::answerType)
                .findFirst();
    }

    public static List<String> visibleQuestionContents() {
        return QUESTIONS.stream()
                .filter(AiStarterQuestion::visible)
                .map(AiStarterQuestion::content)
                .toList();
    }

    public static String contentOf(AiCuratedAnswerType answerType) {
        return definitionOf(answerType).content();
    }

    public static boolean isVisible(AiCuratedAnswerType answerType) {
        return definitionOf(answerType).visible();
    }

    private static AiStarterQuestion definitionOf(AiCuratedAnswerType answerType) {
        return QUESTIONS.stream()
                .filter(question -> question.answerType() == answerType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "등록되지 않은 전용 답변 유형입니다: " + answerType));
    }
}
