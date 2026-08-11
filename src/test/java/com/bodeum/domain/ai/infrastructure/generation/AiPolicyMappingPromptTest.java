package com.bodeum.domain.ai.infrastructure.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class AiPolicyMappingPromptTest {

    @Test
    void explainsOfficialParentPolicyInsteadOfRejectingChildActivitySupport() throws IOException {
        assertPolicyMapping("prompts/ai-rag-system-prompt.txt");
        assertPolicyMapping("prompts/ai-external-search-system-prompt.txt");
    }

    private void assertPolicyMapping(String path) throws IOException {
        String prompt = new ClassPathResource(path)
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(prompt).contains(
                "공식 상위 제도의 적용 대상에 포함되면 제도가 없다고 답하지 말고",
                "장애아동 대상 제도 질문",
                "연령이나 대상 표현만으로 수급 자격을 확정하지 마세요",
                "목적이 다른 돌봄·의료·재활 서비스의 자료로 대체",
                "답변 본문에 \"제공된 자료\", \"참고자료\", \"자료 범위\"",
                "전국 공통 활동지원 제도를 먼저 안내",
                "사용자 활동 지역의 동일 목적 추가지원 사업",
                "지역 사업의 기준을 전국 공통 기준처럼 혼합하지 마세요",
                "연령만으로 수급 자격이 확정되는 것처럼 표현하지 마세요",
                "대상 연령, 아동 기준, 선정 점수가 명시되어 있으면 이를 생략하지 말고",
                "별도 제도임을 명시"
        );
    }
}
