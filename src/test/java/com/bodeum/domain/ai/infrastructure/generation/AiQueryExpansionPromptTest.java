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
                "최대 3개",
                "우리 동네",
                "서비스, 지원사업, 프로그램",
                "LOCAL_RESOURCE",
                "지역 표현이 없는 서비스·지원사업 질문",
                "장애인 활동지원서비스 아동 신청 대상 및 신청 방법",
                "목적이 다른 서비스는 검색 질의의 대체 제도로 사용하지 마세요"
        );
    }
}
