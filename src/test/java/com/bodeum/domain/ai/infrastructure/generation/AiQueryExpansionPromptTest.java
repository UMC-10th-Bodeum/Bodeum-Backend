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
                "장애아동을 대상으로 하는 제도, 서비스 또는 지원사업 질문",
                "대상 표현만 \"장애아동\"에서 \"장애인\"으로 확장",
                "서비스명, 질문의 목적, 신청 조건 등 나머지 의미와 조건은 변경하지 마세요",
                "원래 제도와 동일한 제도라고 단정하지 마세요",
                "목적이 다른 서비스는 검색 질의의 대체 제도로 사용하지 마세요"
        );
    }
}
