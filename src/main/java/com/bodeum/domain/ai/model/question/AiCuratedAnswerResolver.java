package com.bodeum.domain.ai.model.question;

import java.util.Optional;

/**
 * LLM 질문 의도를 검증된 전용 답변 키로 변환한다.
 */
public final class AiCuratedAnswerResolver {

    private AiCuratedAnswerResolver() {
    }

    public static Optional<AiCuratedAnswerType> resolve(AiQuestionIntent intent) {
        return switch (intent) {
            case WELFARE_SITES -> Optional.of(AiCuratedAnswerType.WELFARE_SITES);
            case LOCAL_REHAB_CENTERS -> Optional.of(
                    AiCuratedAnswerType.LOCAL_REHAB_CENTERS);
            case CHILD_MEDICAL_SUPPORT -> Optional.of(
                    AiCuratedAnswerType.CHILD_MEDICAL_SUPPORT);
            case DIAGNOSIS_FIRST_STEPS -> Optional.of(
                    AiCuratedAnswerType.DIAGNOSIS_FIRST_STEPS);
            case VOUCHER_APPLICATION -> Optional.of(
                    AiCuratedAnswerType.VOUCHER_APPLICATION);
            default -> Optional.empty();
        };
    }
}
