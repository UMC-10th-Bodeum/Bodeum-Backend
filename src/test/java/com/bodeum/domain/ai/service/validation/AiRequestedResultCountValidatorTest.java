package com.bodeum.domain.ai.service.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AiRequestedResultCountValidatorTest {

    private final AiRequestedResultCountValidator validator =
            new AiRequestedResultCountValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "특수학교 -1개", "특수학교 - 6개", "사이트 0개", "센터 −2곳",
            "특수학교 -999999999999999999999999개"
    })
    void rejectsNonPositiveExplicitCounts(String question) {
        assertThat(validator.hasNonPositiveCount(question)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"특수학교 1개", "특수학교 15개", "특수학교 알려줘", "6세 아동"})
    void acceptsPositiveOrMissingCounts(String question) {
        assertThat(validator.hasNonPositiveCount(question)).isFalse();
    }
}
