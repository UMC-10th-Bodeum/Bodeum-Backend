package com.bodeum.domain.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiSiteListAnswerValidatorTest {

    private final AiSiteListAnswerValidator validator = new AiSiteListAnswerValidator();

    @Test
    void recognizesImplicitWelfareSiteListQuestion() {
        assertThat(validator.requiresValidation("성남에서 알아두면 좋은 복지사이트"))
                .isTrue();
    }

    @Test
    void doesNotTreatSiteUsageQuestionAsListRequest() {
        assertThat(validator.requiresValidation("복지로 사이트 로그인 방법을 알려줘"))
                .isFalse();
    }
}
