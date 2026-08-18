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
                "두 복지관의 서비스와 비용을 비교해줘\" -> NONE",
                "서로 다른 둘 이상의 후속 안내 항목을 선택지로 제안한 뒤",
                "원하는 항목을 특정하지 않고 수락하면 needsClarification을 true",
                "여러 항목을 하나의 resolvedQuestion으로 합치지 마세요.",
                "후속 안내 항목을 하나만 제안했거나 사용자가 원하는 항목을 명시했다면",
                "사이트 또는 홈페이지 목록을 명시적으로 요청했다면",
                "복지사업, 서비스 또는 기관 목록 요청으로 바꾸지 마세요.",
                "가입 가능한 복지 사이트 추천해줘",
                "siteListRequest=true",
                "복지로 사이트 로그인 방법을 알려줘",
                "siteListRequest=false",
                "resolvedContext에는 현재 질문을 독립적으로 검색하는 데 필요한 구조화 문맥",
                "변경하지 않은 topic·region·filters·requestedInformation은 유지"
        );
    }

    @Test
    void distinguishesStructuredResourceListsFromInstitutionExplanations()
            throws IOException {
        String prompt = new ClassPathResource(
                "prompts/ai-question-intent-classifier-system-prompt.txt"
        ).getContentAsString(StandardCharsets.UTF_8);

        assertThat(prompt).contains(
                "resourceListRequest=true",
                "resourceListRequest=false",
                "수원 특수학교 5개",
                "특정 기관의 이용 방법",
                "resolvedContext.requestedInformation이 자원 목록인 경우에만",
                "같은 설명을 확장하는 후속 요청으로 판단하고 resourceListRequest=false"
        );
    }

    @Test
    void distinguishesAdditionalFilterAndDetailFollowUps() throws IOException {
        String prompt = new ClassPathResource(
                "prompts/ai-question-intent-classifier-system-prompt.txt"
        ).getContentAsString(StandardCharsets.UTF_8);

        assertThat(prompt).contains(
                "referencesPreviousContext",
                "excludePreviousResults",
                "더 알려줘",
                "직전 답변이 목록",
                "직전 답변이 제도나 방법 설명",
                "사용자 프로필 지역",
                "현재 질문에 지역을 명시",
                "현재 대화의 검색 문맥만 변경"
        );
    }
}
