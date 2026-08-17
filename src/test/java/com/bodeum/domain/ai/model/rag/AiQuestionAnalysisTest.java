package com.bodeum.domain.ai.model.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.ai.enums.AiQuestionIntent;
import com.bodeum.domain.ai.enums.AiSearchScope;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiQuestionAnalysisTest {

    @Test
    void preservesSearchScopeForGeneralRagQuestion() {
        AiQuestionAnalysis analysis = AiQuestionAnalysis.forQuestion(
                "우리 지역 특수학교 알려줘",
                AiQuestionIntent.NONE,
                AiSearchScope.LOCAL_RESOURCE,
                List.of("수원시 특수학교")
        );

        assertThat(analysis.searchScope()).isEqualTo(AiSearchScope.LOCAL_RESOURCE);
        assertThat(analysis.retrievalQueries())
                .containsExactly("우리 지역 특수학교 알려줘", "수원시 특수학교");
    }

    @Test
    void normalizesAndLimitsRetrievalQueries() {
        AiQuestionAnalysis analysis = new AiQuestionAnalysis(
                AiQuestionIntent.NONE,
                List.of(" 원문 ", "공식 제도명", "공식 제도명", "세 번째", "네 번째")
        );

        assertThat(analysis.retrievalQueries())
                .containsExactly("원문", "공식 제도명", "세 번째");
    }

    @Test
    void fallsBackToNoneWithNoExpandedQuery() {
        AiQuestionAnalysis analysis = AiQuestionAnalysis.fallback();

        assertThat(analysis.intent()).isEqualTo(AiQuestionIntent.NONE);
        assertThat(analysis.retrievalQueries()).isEmpty();
    }

    @Test
    void alwaysPlacesOriginalQuestionBeforeExpandedQueries() {
        AiQuestionAnalysis analysis = AiQuestionAnalysis.forQuestion(
                " 장애아동 활동지원 서비스 신청 방법 ",
                AiQuestionIntent.NONE,
                List.of(
                        "장애인 활동지원서비스 신청 방법",
                        "장애아동 활동지원 서비스 신청 방법",
                        "장애인 활동지원서비스 아동 신청 대상"
                )
        );

        assertThat(analysis.retrievalQueries()).containsExactly(
                "장애아동 활동지원 서비스 신청 방법",
                "장애인 활동지원서비스 신청 방법",
                "장애인 활동지원서비스 아동 신청 대상"
        );
    }

    @Test
    void fallbackKeepsOriginalQuestion() {
        AiQuestionAnalysis analysis = AiQuestionAnalysis.fallback("원문 질문");

        assertThat(analysis.intent()).isEqualTo(AiQuestionIntent.NONE);
        assertThat(analysis.retrievalQueries()).containsExactly("원문 질문");
    }

    @Test
    void ignoresRetrievalQueriesForNonNoneIntent() {
        AiQuestionAnalysis analysis = AiQuestionAnalysis.forQuestion(
                "진단해줘",
                AiQuestionIntent.MEDICAL_DIAGNOSIS,
                List.of("확장 질의")
        );

        assertThat(analysis.intent()).isEqualTo(AiQuestionIntent.MEDICAL_DIAGNOSIS);
        assertThat(analysis.retrievalQueries()).isEmpty();
    }

    @Test
    void keepsRequestedResultCountFromClassifier() {
        AiQuestionAnalysis analysis = AiQuestionAnalysis.forQuestion(
                "근처 장애인재활센터 열 개 알려줘",
                AiQuestionIntent.NONE,
                AiSearchScope.LOCAL_RESOURCE,
                List.of(),
                10
        );

        assertThat(analysis.requestedResultCount()).isEqualTo(10);
    }

    @Test
    void enablesClarificationOnlyWhenQuestionTextExists() {
        AiQuestionAnalysis analysis = AiQuestionAnalysis.forQuestion(
                "센터를 알려줘",
                AiQuestionIntent.NONE,
                List.of()
        ).withClarification(true, " 어떤 종류의 센터를 찾으시나요? ");

        assertThat(analysis.needsClarification()).isTrue();
        assertThat(analysis.clarificationQuestion())
                .isEqualTo("어떤 종류의 센터를 찾으시나요?");

        AiQuestionAnalysis invalid = analysis.withClarification(true, " ");
        assertThat(invalid.needsClarification()).isFalse();
        assertThat(invalid.clarificationQuestion()).isNull();
    }

    @Test
    void preservesStructuredSiteListRequestAcrossAnalysisUpdates() {
        AiQuestionAnalysis analysis = AiQuestionAnalysis.forQuestion(
                "가입 가능한 복지 사이트 추천해줘",
                AiQuestionIntent.NONE,
                List.of()
        ).withSiteListRequest(true)
                .withClarification(false, null)
                .withResolvedContext(null);

        assertThat(analysis.siteListRequest()).isTrue();
    }

    @Test
    void preservesStructuredResourceListRequestAcrossAnalysisUpdates() {
        AiQuestionAnalysis analysis = AiQuestionAnalysis.forQuestion(
                "수원 특수학교 5개 알려줘",
                AiQuestionIntent.NONE,
                List.of()
        ).withResourceListRequest(true)
                .withClarification(false, null)
                .withResolvedContext(null);

        assertThat(analysis.resourceListRequest()).isTrue();
    }
}
