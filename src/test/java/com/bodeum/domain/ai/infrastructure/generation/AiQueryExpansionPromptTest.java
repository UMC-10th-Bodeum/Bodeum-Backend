package com.bodeum.domain.ai.infrastructure.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class AiQueryExpansionPromptTest {

    @Test
    void preservesOriginalMeaningAndProhibitsEligibilityGuessing() throws IOException {
        String prompt = new ClassPathResource(
                "prompts/ai-query-expansion-system-prompt.txt"
        ).getContentAsString(StandardCharsets.UTF_8);

        assertThat(prompt).contains(
                "사용자 질문의 의미와 조건을 유지",
                "공식 제도명",
                "자격 여부와 사실관계를 추측하지 마세요",
                "최대 3개"
        );
    }
}
