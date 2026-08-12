package com.bodeum.domain.ai.infrastructure.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AiPromptTemplateTest {

    @Test
    void replacesAllRequiredPlaceholders() {
        String result = AiPromptTemplate.replaceRequiredPlaceholder(
                "최대 {{maxResultCount}}개, 다시 {{maxResultCount}}개",
                "{{maxResultCount}}",
                "10"
        );

        assertThat(result).isEqualTo("최대 10개, 다시 10개");
    }

    @Test
    void failsWhenRequiredPlaceholderIsMissing() {
        assertThatThrownBy(() -> AiPromptTemplate.replaceRequiredPlaceholder(
                "최대 결과 개수를 안내하세요.",
                "{{maxResultCount}}",
                "10"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("{{maxResultCount}}");
    }
}
