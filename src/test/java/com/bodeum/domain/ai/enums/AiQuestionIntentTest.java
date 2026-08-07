package com.bodeum.domain.ai.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiQuestionIntentTest {

    @Test
    void mapsRecommendationIntentsToStarterQuestionTypes() {
        assertThat(AiQuestionIntent.WELFARE_SITES.starterQuestionType())
                .contains(AiStarterQuestionType.WELFARE_SITES);
        assertThat(AiQuestionIntent.LOCAL_REHAB_CENTERS.starterQuestionType())
                .contains(AiStarterQuestionType.LOCAL_REHAB_CENTERS);
        assertThat(AiQuestionIntent.CHILD_MEDICAL_SUPPORT.starterQuestionType())
                .contains(AiStarterQuestionType.CHILD_MEDICAL_SUPPORT);
        assertThat(AiQuestionIntent.DIAGNOSIS_FIRST_STEPS.starterQuestionType())
                .contains(AiStarterQuestionType.DIAGNOSIS_FIRST_STEPS);
        assertThat(AiQuestionIntent.VOUCHER_APPLICATION.starterQuestionType())
                .contains(AiStarterQuestionType.VOUCHER_APPLICATION);
    }

    @Test
    void providesGuidanceOnlyForSafetyIntents() {
        assertThat(AiQuestionIntent.MEDICAL_DIAGNOSIS.safetyGuidance())
                .hasValueSatisfying(content -> assertThat(content)
                        .contains("진단해드릴 수 없습니다"));
        assertThat(AiQuestionIntent.LEGAL_ADVICE.safetyGuidance())
                .hasValueSatisfying(content -> assertThat(content)
                        .contains("법률 자문을 제공해드릴 수 없습니다"));
        assertThat(AiQuestionIntent.INSTITUTION_EVALUATION.safetyGuidance())
                .hasValueSatisfying(content -> assertThat(content)
                        .contains("주관적으로 평가해드릴 수 없습니다"));
        assertThat(AiQuestionIntent.NONE.safetyGuidance()).isEmpty();
    }
}
