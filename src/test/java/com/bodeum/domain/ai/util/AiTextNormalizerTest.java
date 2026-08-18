package com.bodeum.domain.ai.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiTextNormalizerTest {

    @Test
    void trimsBlankValuesToNull() {
        assertThat(AiTextNormalizer.trimToNull("  ")).isNull();
        assertThat(AiTextNormalizer.trimToNull("  특수학교  ")).isEqualTo("특수학교");
    }

    @Test
    void removesAllWhitespace() {
        assertThat(AiTextNormalizer.removeWhitespace(" 수원  특수학교\n알려줘 "))
                .isEqualTo("수원특수학교알려줘");
    }

    @Test
    void normalizesQuestionSpacingAndTrailingPunctuation() {
        assertThat(AiTextNormalizer.normalizeQuestionSpacing(
                "  수원   특수학교 알려줘?!~  "))
                .isEqualTo("수원 특수학교 알려줘");
    }
}
