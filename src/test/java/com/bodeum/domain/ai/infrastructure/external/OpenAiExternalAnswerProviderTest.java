package com.bodeum.domain.ai.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpenAiExternalAnswerProviderTest {

    @Test
    void detectsExplicitNoEvidenceMarker() {
        assertThat(OpenAiExternalAnswerProvider.isNoEvidenceAnswer("[[NO_EVIDENCE]]"))
                .isTrue();
    }

    @Test
    void detectsNaturalLanguageNoEvidenceAnswer() {
        assertThat(OpenAiExternalAnswerProvider.isNoEvidenceAnswer(
                "허용된 사이트에서 김치찌개 레시피를 찾지 못했습니다."))
                .isTrue();
    }

    @Test
    void acceptsGroundedAnswer() {
        assertThat(OpenAiExternalAnswerProvider.isNoEvidenceAnswer(
                "한국장애인부모회에서 장애인 가족 지원 사업 정보를 확인할 수 있습니다."))
                .isFalse();
    }
}
