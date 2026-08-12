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
                "답변 본문에 \"제공된 자료\", \"참고자료\"",
                "\"참고된 내용\", \"확인된 내용에 따르면\"",
                "\"여기서 확인되는\", \"확인되는 자료에는\"",
                "전국 공통 활동지원 제도를 먼저 안내",
                "사용자 활동 지역의 동일 목적 추가지원 사업",
                "내부 검색 결과를 설명하지 말고",
                "공식 제도명 또는 서비스명을 직접 안내하세요",
                "두 표현의 관계를 근거에서 확인할 수 있다면",
                "관계가 명확하지 않으면 같은 제도라고 단정하지 마세요",
                "공식 상위 제도인 \"장애인활동지원\"을 우선 안내",
                "장애아동에게 적용되는 연령·선정 기준",
                "신청 장소, 문의처, 준비 서류",
                "해당 서비스의 신청 절차만 안내하세요",
                "다른 제도·서비스의 신청 절차를 해당 서비스의 신청 단계처럼 혼합하지 마세요",
                "질문과 직접 관계없는 별도 제도",
                "서로 다른 제도·서비스의 신청 절차",
                "하나의 연속된 신청 절차처럼 합치지 마세요",
                "\"자료에 나오지 않는다\"고 설명하지 말고",
                "장애인활동지원의 공통 신청 절차",
                "실제 신청 가능 여부는 연령과 선정 기준",
                "지역 사업의 기준을 전국 공통 기준처럼 혼합하지 마세요",
                "연령만으로 수급 자격이 확정되는 것처럼 표현하지 마세요",
                "대상 연령, 아동 기준, 선정 점수가 명시되어 있으면 이를 생략하지 말고",
                "별도 제도임을 명시"
        );
    }
}
