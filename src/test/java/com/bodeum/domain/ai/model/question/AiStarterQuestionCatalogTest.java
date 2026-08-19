package com.bodeum.domain.ai.model.question;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiStarterQuestionCatalogTest {

    @Test
    void recognizesSpacingAndPoliteEndingVariants() {
        assertThat(AiStarterQuestionCatalog.findAnswerType(
                "참고하면 좋을 복지 사이트 알려 주세요."
        )).contains(AiCuratedAnswerType.WELFARE_SITES);
        assertThat(AiStarterQuestionCatalog.findAnswerType(
                "우리 동네 재활센터 추천해주세요"
        )).contains(AiCuratedAnswerType.LOCAL_REHAB_CENTERS);
        assertThat(AiStarterQuestionCatalog.findAnswerType(
                "우리 동네 재활 센터를 추천해 주세요."
        )).contains(AiCuratedAnswerType.LOCAL_REHAB_CENTERS);
        assertThat(AiStarterQuestionCatalog.findAnswerType(
                "재활 센터를 추천해 주세요."
        )).isEmpty();
        assertThat(AiStarterQuestionCatalog.findAnswerType(
                "장애아동 의료비 지원이 궁금합니다"
        )).contains(AiCuratedAnswerType.CHILD_MEDICAL_SUPPORT);
        assertThat(AiStarterQuestionCatalog.findAnswerType(
                "바우처 신청 방법 알려주세요!"
        )).contains(AiCuratedAnswerType.VOUCHER_APPLICATION);
        assertThat(AiStarterQuestionCatalog.findAnswerType(
                "자폐 관련 공식 사이트 알려주세요."
        )).contains(AiCuratedAnswerType.AUTISM_INFO_SITES);
    }

    @Test
    void recognizesUnambiguousAliases() {
        assertThat(AiStarterQuestionCatalog.findAnswerType("복지사이트를 알려줘"))
                .contains(AiCuratedAnswerType.WELFARE_SITES);
        assertThat(AiStarterQuestionCatalog.findAnswerType("장애아동 병원비 지원 알려줘"))
                .contains(AiCuratedAnswerType.CHILD_MEDICAL_SUPPORT);
        assertThat(AiStarterQuestionCatalog.findAnswerType("장애 진단 후 뭘 먼저 해야 해?"))
                .contains(AiCuratedAnswerType.DIAGNOSIS_FIRST_STEPS);
        assertThat(AiStarterQuestionCatalog.findAnswerType("바우처 신청 방법을 알려주세요."))
                .isEmpty();
        assertThat(AiStarterQuestionCatalog.findAnswerType("방법을 알려줘"))
                .isEmpty();
    }

    @Test
    void recognizesRelativeRegionRehabCenterListQuestions() {
        assertThat(AiStarterQuestionCatalog.findAnswerType("근처 재활센터를 알려줘"))
                .contains(AiCuratedAnswerType.LOCAL_REHAB_CENTERS);
        assertThat(AiStarterQuestionCatalog.findAnswerType("주변 재활 센터 추천해주세요"))
                .contains(AiCuratedAnswerType.LOCAL_REHAB_CENTERS);
        assertThat(AiStarterQuestionCatalog.findAnswerType("가까운 재활센터 알려줘"))
                .contains(AiCuratedAnswerType.LOCAL_REHAB_CENTERS);
        assertThat(AiStarterQuestionCatalog.findAnswerType("근처 재활센터 비용 알려줘"))
                .isEmpty();
        assertThat(AiStarterQuestionCatalog.findAnswerType("재활센터 신청 방법 알려줘"))
                .isEmpty();
    }

    @Test
    void exposesOnlyVisibleStarterQuestions() {
        assertThat(AiStarterQuestionCatalog.visibleQuestionContents())
                .contains(AiStarterQuestionCatalog.contentOf(
                        AiCuratedAnswerType.WELFARE_SITES))
                .doesNotContain(AiStarterQuestionCatalog.contentOf(
                        AiCuratedAnswerType.AUTISM_INFO_SITES));
        assertThat(AiStarterQuestionCatalog.isVisible(
                AiCuratedAnswerType.AUTISM_INFO_SITES)).isFalse();
    }
}
