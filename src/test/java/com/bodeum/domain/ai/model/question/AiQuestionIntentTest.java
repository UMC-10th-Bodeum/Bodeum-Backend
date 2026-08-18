package com.bodeum.domain.ai.model.question;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiQuestionIntentTest {

    @Test
    void mapsRecommendationIntentsToCuratedAnswers() {
        assertThat(AiCuratedAnswerResolver.resolve(AiQuestionIntent.WELFARE_SITES))
                .contains(AiCuratedAnswerType.WELFARE_SITES);
        assertThat(AiCuratedAnswerResolver.resolve(AiQuestionIntent.LOCAL_REHAB_CENTERS))
                .contains(AiCuratedAnswerType.LOCAL_REHAB_CENTERS);
        assertThat(AiCuratedAnswerResolver.resolve(AiQuestionIntent.CHILD_MEDICAL_SUPPORT))
                .contains(AiCuratedAnswerType.CHILD_MEDICAL_SUPPORT);
        assertThat(AiCuratedAnswerResolver.resolve(AiQuestionIntent.DIAGNOSIS_FIRST_STEPS))
                .contains(AiCuratedAnswerType.DIAGNOSIS_FIRST_STEPS);
        assertThat(AiCuratedAnswerResolver.resolve(AiQuestionIntent.VOUCHER_APPLICATION))
                .contains(AiCuratedAnswerType.VOUCHER_APPLICATION);
    }

    @Test
    void providesGuidanceOnlyForSafetyIntents() {
        assertThat(AiSafetyGuidanceResolver.resolve(AiQuestionIntent.MEDICAL_DIAGNOSIS))
                .hasValueSatisfying(content -> assertThat(content)
                        .contains("진단해드릴 수 없습니다"));
        assertThat(AiSafetyGuidanceResolver.resolve(AiQuestionIntent.LEGAL_ADVICE))
                .hasValueSatisfying(content -> assertThat(content)
                        .contains("법률 자문을 제공해드릴 수 없습니다"));
        assertThat(AiSafetyGuidanceResolver.resolve(AiQuestionIntent.INSTITUTION_EVALUATION))
                .hasValueSatisfying(content -> assertThat(content)
                        .contains("주관적으로 평가해드릴 수 없습니다"));
        assertThat(AiSafetyGuidanceResolver.resolve(AiQuestionIntent.NONE)).isEmpty();
    }
}
