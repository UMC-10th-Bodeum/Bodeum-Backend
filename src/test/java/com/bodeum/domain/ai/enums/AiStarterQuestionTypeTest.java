package com.bodeum.domain.ai.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiStarterQuestionTypeTest {

    @Test
    void recognizesSpacingAndPoliteEndingVariants() {
        assertThat(AiStarterQuestionType.fromQuestion(
                "참고하면 좋을 복지 사이트 알려 주세요."
        )).contains(AiStarterQuestionType.WELFARE_SITES);
        assertThat(AiStarterQuestionType.fromQuestion(
                "우리 동네 재활센터 추천해주세요"
        )).contains(AiStarterQuestionType.LOCAL_REHAB_CENTERS);
        assertThat(AiStarterQuestionType.fromQuestion(
                "우리 동네 재활 센터를 추천해 주세요."
        )).contains(AiStarterQuestionType.LOCAL_REHAB_CENTERS);
        assertThat(AiStarterQuestionType.fromQuestion(
                "재활 센터를 추천해 주세요."
        )).isEmpty();
        assertThat(AiStarterQuestionType.fromQuestion(
                "장애아동 의료비 지원이 궁금합니다"
        )).contains(AiStarterQuestionType.CHILD_MEDICAL_SUPPORT);
        assertThat(AiStarterQuestionType.fromQuestion(
                "바우처 신청 방법 알려주세요!"
        )).contains(AiStarterQuestionType.VOUCHER_APPLICATION);
        assertThat(AiStarterQuestionType.fromQuestion(
                "자폐 관련 공식 사이트 알려주세요."
        )).contains(AiStarterQuestionType.AUTISM_INFO_SITES);
    }

    @Test
    void recognizesUnambiguousSuggestedQuestionAliases() {
        assertThat(AiStarterQuestionType.fromQuestion("복지사이트를 알려줘"))
                .contains(AiStarterQuestionType.WELFARE_SITES);
        assertThat(AiStarterQuestionType.fromQuestion("장애아동 병원비 지원 알려줘"))
                .contains(AiStarterQuestionType.CHILD_MEDICAL_SUPPORT);
        assertThat(AiStarterQuestionType.fromQuestion("장애 진단 후 뭘 먼저 해야 해?"))
                .contains(AiStarterQuestionType.DIAGNOSIS_FIRST_STEPS);
        assertThat(AiStarterQuestionType.fromQuestion("바우처 신청 방법을 알려주세요."))
                .contains(AiStarterQuestionType.VOUCHER_APPLICATION);
        assertThat(AiStarterQuestionType.fromQuestion("발달재활서비스 바우처 신청 방법 알려줘"))
                .contains(AiStarterQuestionType.VOUCHER_APPLICATION);
    }

    @Test
    void doesNotRecognizeAmbiguousShortQuestionAsAlias() {
        assertThat(AiStarterQuestionType.fromQuestion("방법을 알려줘"))
                .isEmpty();
    }
}
