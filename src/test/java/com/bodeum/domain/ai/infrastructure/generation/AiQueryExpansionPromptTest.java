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
                "최대 {{maxQueryCount}}개",
                "우리 동네",
                "서비스, 지원사업, 프로그램",
                "LOCAL_ONLY",
                "지역 표현이 없는 서비스·지원사업 질문",
                "장애아동을 대상으로 하는 제도, 서비스 또는 지원사업 질문",
                "대상 표현만 \"장애아동\"에서 \"장애인\"으로 확장",
                "서비스명, 질문의 목적, 신청 조건 등 나머지 의미와 조건은 변경하지 마세요",
                "원래 제도와 동일한 제도라고 단정하지 마세요",
                "목적이 다른 서비스는 검색 질의의 대체 제도로 사용하지 마세요"
        );
    }

    @Test
    void normalizesExplicitAndMaximumResultCountExpressions() throws IOException {
        String prompt = new ClassPathResource(
                "prompts/ai-question-intent-classifier-system-prompt.txt"
        ).getContentAsString(StandardCharsets.UTF_8);

        assertThat(prompt).contains(
                "숫자, 한글 수사, 단위 표현이 달라도",
                "다섯 군데",
                "한 스무 곳",
                "최대한 많이",
                "가능한 곳 전부",
                "한 번 응답 최대치인 {{maxResultCount}}",
                "개수나 최대 범위 요청이 없다면 null"
        );
    }

    @Test
    void keepsRegionlessResourceQuestionsNationwideWithProfilePriority() throws IOException {
        String prompt = new ClassPathResource(
                "prompts/ai-query-expansion-system-prompt.txt"
        ).getContentAsString(StandardCharsets.UTF_8);

        assertThat(prompt).contains(
                "지역 표현이 없는 기관·시설·학교·병원·센터 질문",
                "REGION_PRIORITY",
                "전국을 검색 범위로 유지",
                "검색 범위를 제한하지 않고 결과 우선순위에만 사용"
        );
    }

    @Test
    void resolvesIncompleteFollowUpQuestionsFromPreviousConversation() throws IOException {
        String prompt = new ClassPathResource(
                "prompts/ai-question-intent-classifier-system-prompt.txt"
        ).getContentAsString(StandardCharsets.UTF_8);

        assertThat(prompt).contains(
                "resolvedQuestion",
                "referencesPreviousContext",
                "독립적인 새 주제이면 false",
                "같은 대화방에 이전 메시지가 있다는 이유로",
                "직전 대화 없이는 의미가 불완전한 후속 질문",
                "대상·지역·자원 유형·조건을 복원",
                "독립적으로 검색하고 답변할 수 있는 하나의 질문",
                "새로 추가하거나 변경한 조건은 반영",
                "변경하지 않은 이전 조건은 유지"
        );
    }

    @Test
    void limitsInfoSubCategoryToSearchableVectorData() throws IOException {
        String prompt = new ClassPathResource(
                "prompts/ai-question-intent-classifier-system-prompt.txt"
        ).getContentAsString(StandardCharsets.UTF_8);

        assertThat(prompt).contains(
                "infoSubCategory",
                "SPECIAL_SCHOOL",
                "EMERGENCY_CLINIC",
                "STANDARD_WORKPLACE",
                "*_ETC",
                "GENERAL_HOSPITAL",
                "YOUTH_CENTER",
                "KEAD_JOB"
        );
    }

    @Test
    void definesStructuredRequiredConceptsWithoutUsingThemAsCategoryFilters()
            throws IOException {
        String prompt = new ClassPathResource(
                "prompts/ai-question-intent-classifier-system-prompt.txt"
        ).getContentAsString(StandardCharsets.UTF_8);

        assertThat(prompt).contains(
                "searchGoal",
                "requiredConcepts",
                "retrievalQuery",
                "matchTerms",
                "excludeTerms",
                "requiresUserRegion",
                "검색 범위를 제한하는 카테고리처럼 사용하지 마세요",
                "센터·제공기관을 묻는 질문",
                "소식·공지 질문"
        );
    }

    @Test
    void asksForClarificationOnlyWhenMissingInformationChangesSearchTarget()
            throws IOException {
        String prompt = new ClassPathResource(
                "prompts/ai-question-intent-classifier-system-prompt.txt"
        ).getContentAsString(StandardCharsets.UTF_8);

        assertThat(prompt).contains(
                "needsClarification",
                "clarificationQuestion",
                "꼭 필요한 정보 하나만",
                "프로필의 활동 지역으로 보완할 수 있는 지역 생략",
                "불필요하게 되묻지 마세요",
                "센터를 알려줘",
                "재활센터를 알려줘"
        );
    }
}
