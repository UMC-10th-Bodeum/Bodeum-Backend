package com.bodeum.domain.ai.infrastructure.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class AiQuestionIntentPromptTest {

    @Test
    void distinguishesSafetyRequestsFromInformationalQuestions() throws IOException {
        String prompt = new ClassPathResource(
                "prompts/ai-question-intent-classifier-system-prompt.txt"
        ).getContentAsString(StandardCharsets.UTF_8);

        assertThat(prompt).contains(
                "MEDICAL_DIAGNOSIS",
                "LEGAL_ADVICE",
                "INSTITUTION_EVALUATION",
                "우리 지역 특수학교 알려줘",
                "우리 동네 장애인복지관 알려줘",
                "LOCAL_REHAB_CENTERS로 분류하지 말고 NONE",
                "어떤 질환인지 판단해주지 말고 병원을 추천해줘\" -> NONE",
                "소송 절차와 필요한 서류를 알려줘\" -> NONE",
                "두 복지관의 서비스와 비용을 비교해줘\" -> NONE"
        );
    }
}
