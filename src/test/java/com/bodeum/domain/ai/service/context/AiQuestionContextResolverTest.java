package com.bodeum.domain.ai.service.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.model.question.AiQuestionIntent;
import com.bodeum.domain.ai.model.question.AiResultType;
import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.ai.model.context.AiConversationContext;
import com.bodeum.domain.ai.model.context.AiResolvedContext;
import com.bodeum.domain.ai.model.rag.AiQuestionAnalysis;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.service.port.AiQuestionIntentClassifier;
import com.bodeum.domain.region.entity.Region;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AiQuestionContextResolverTest {

    @Test
    void ignoresClassifierDefaultCountWhenStandaloneResourceQuestionHasNoCount() {
        String question = "과천시 특수학교를 알려줘";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        var gwacheon = new AiQuestionRegionResolver.RegionResolution(
                AiQuestionRegionResolver.RegionResolution.Status.RESOLVED,
                Region.create("경기도", "과천시"),
                "경기도 과천시",
                List.of());
        when(regionResolver.resolve(any(String.class), any(AiUserProfile.class)))
                .thenReturn(gwacheon);
        when(classifier.analyze(any(), any(), any(), any(), any()))
                .thenReturn(new AiQuestionAnalysis(
                        AiQuestionIntent.NONE,
                        AiSearchScope.LOCAL_ONLY,
                        List.of(),
                        10,
                        question,
                        com.bodeum.domain.info.entity.enums.InfoSubCategory.SPECIAL_SCHOOL,
                        null,
                        List.of(),
                        false,
                        null,
                        new AiResolvedContext(
                                "특수학교", null, Map.of(), "목록", 10,
                                AiResultType.RESOURCE_LIST),
                        false,
                        true,
                        false,
                        false));

        var context = resolver.resolve(question, profile, AiConversationContext.empty());

        assertThat(context.requestedResultCount()).isNull();
        assertThat(context.resolvedContext().requestedResultCount()).isNull();
        assertThat(context.resultType()).isEqualTo(AiResultType.RESOURCE_LIST);
    }

    @Test
    void keepsExplicitCountForStandaloneResourceQuestion() {
        String question = "부산 특수학교 7개를 알려줘";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        var busan = new AiQuestionRegionResolver.RegionResolution(
                AiQuestionRegionResolver.RegionResolution.Status.RESOLVED,
                Region.create("부산광역시", null),
                "부산광역시",
                List.of());
        when(regionResolver.resolve(any(String.class), any(AiUserProfile.class)))
                .thenReturn(busan);
        when(classifier.analyze(any(), any(), any(), any(), any()))
                .thenReturn(AiQuestionAnalysis.forQuestion(
                                question, AiQuestionIntent.NONE,
                                AiSearchScope.LOCAL_ONLY, List.of(), 10)
                        .withResourceListRequest(true));

        var context = resolver.resolve(question, profile, AiConversationContext.empty());

        assertThat(context.requestedResultCount()).isEqualTo(7);
        assertThat(context.resolvedContext().requestedResultCount()).isEqualTo(7);
    }

    @Test
    void ignoresClassifierDefaultCountWhenStandaloneSiteQuestionHasNoCount() {
        String question = "장애아동 공식 사이트를 알려줘";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        AiResolvedContext analyzedContext = new AiResolvedContext(
                "장애아동 공식 사이트", null, Map.of(), "목록", 10,
                AiResultType.SITE_LIST);
        when(regionResolver.resolve(question, profile))
                .thenReturn(AiQuestionRegionResolver.RegionResolution.notFound());
        when(classifier.analyze(any(), any(), any(), any(), any()))
                .thenReturn(AiQuestionAnalysis.forQuestion(
                                question, AiQuestionIntent.NONE,
                                AiSearchScope.NATIONWIDE, List.of(), 10)
                        .withSiteListRequest(true)
                        .withResolvedContext(analyzedContext));

        var context = resolver.resolve(question, profile, AiConversationContext.empty());

        assertThat(context.resultType()).isEqualTo(AiResultType.SITE_LIST);
        assertThat(context.requestedResultCount()).isNull();
        assertThat(context.resolvedContext().requestedResultCount()).isNull();
    }

    @Test
    void routesSpecialSchoolListToStructuredSearchWhenLlmFallsBackToGeneral() {
        String question = "특수학교를 알려줘";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        when(regionResolver.resolve(question, profile))
                .thenReturn(AiQuestionRegionResolver.RegionResolution.notFound());
        when(classifier.analyze(any(), any(), any(), any(), any()))
                .thenReturn(AiQuestionAnalysis.forQuestion(
                        question, AiQuestionIntent.NONE,
                        AiSearchScope.REGION_PRIORITY, List.of()));

        var context = resolver.resolve(question, profile, AiConversationContext.empty());

        assertThat(context.resultType()).isEqualTo(AiResultType.RESOURCE_LIST);
        assertThat(context.searchScope()).isEqualTo(AiSearchScope.REGION_PRIORITY);
        assertThat(context.searchProfile().infoSubCategory())
                .isEqualTo(com.bodeum.domain.info.entity.enums.InfoSubCategory.SPECIAL_SCHOOL);
    }

    @Test
    void keepsSpecialSchoolDetailQuestionAsDocumentAnswerWhenLlmFallsBackToGeneral() {
        String question = "특수학교 입학 방법을 알려줘";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        when(regionResolver.resolve(question, profile))
                .thenReturn(AiQuestionRegionResolver.RegionResolution.notFound());
        when(classifier.analyze(any(), any(), any(), any(), any()))
                .thenReturn(AiQuestionAnalysis.forQuestion(
                        question, AiQuestionIntent.NONE,
                        AiSearchScope.REGION_PRIORITY, List.of()));

        var context = resolver.resolve(question, profile, AiConversationContext.empty());

        assertThat(context.resultType()).isEqualTo(AiResultType.DOCUMENT_ANSWER);
    }

    @Test
    void keepsSpecialSchoolApplicationQuestionAsDocumentAnswerWhenItContainsWhere() {
        String question = "특수학교 지원은 어디서 해?";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        when(regionResolver.resolve(question, profile))
                .thenReturn(AiQuestionRegionResolver.RegionResolution.notFound());
        when(classifier.analyze(any(), any(), any(), any(), any()))
                .thenReturn(AiQuestionAnalysis.forQuestion(
                        question, AiQuestionIntent.NONE,
                        AiSearchScope.REGION_PRIORITY, List.of()));

        var context = resolver.resolve(question, profile, AiConversationContext.empty());

        assertThat(context.resultType()).isEqualTo(AiResultType.DOCUMENT_ANSWER);
    }

    @Test
    void keepsSpecialEducationSupportCenterQuestionAsResourceList() {
        String question = "특수교육지원센터를 알려줘";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        when(regionResolver.resolve(question, profile))
                .thenReturn(AiQuestionRegionResolver.RegionResolution.notFound());
        when(classifier.analyze(any(), any(), any(), any(), any()))
                .thenReturn(AiQuestionAnalysis.forQuestion(
                        question, AiQuestionIntent.NONE,
                        AiSearchScope.REGION_PRIORITY, List.of()));

        var context = resolver.resolve(question, profile, AiConversationContext.empty());

        assertThat(context.resultType()).isEqualTo(AiResultType.RESOURCE_LIST);
        assertThat(context.searchProfile().infoSubCategory())
                .isEqualTo(com.bodeum.domain.info.entity.enums.InfoSubCategory
                        .SPECIAL_EDU_SUPPORT);
    }

    @Test
    void promotesRelativeLocalResourceQuestionEvenWhenLlmReturnsGeneral() {
        String question = "근처 장애인재활센터 알려줘";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        when(regionResolver.resolve(question, profile))
                .thenReturn(AiQuestionRegionResolver.RegionResolution.notFound());
        when(classifier.analyze(any(), any(), any(), any(), any()))
                .thenReturn(AiQuestionAnalysis.forQuestion(
                        question, AiQuestionIntent.NONE,
                        AiSearchScope.REGION_PRIORITY, List.of()));

        var context = resolver.resolve(question, profile, AiConversationContext.empty());

        assertThat(context.searchScope()).isEqualTo(AiSearchScope.LOCAL_ONLY);
        assertThat(context.searchProfile().region()).isEqualTo("경기도 수원시");
    }

    @Test
    void keepsUserProfileRegionSeparateFromQuestionSearchRegion() {
        String question = "부산 특수학교 알려줘";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        var busan = new AiQuestionRegionResolver.RegionResolution(
                AiQuestionRegionResolver.RegionResolution.Status.RESOLVED,
                Region.create("부산광역시", "해운대구"),
                "부산광역시",
                List.of());
        when(regionResolver.resolve(question, profile)).thenReturn(busan);
        when(classifier.analyze(any(), any(), any(), any(), any()))
                .thenReturn(AiQuestionAnalysis.forQuestion(
                        question, AiQuestionIntent.NONE,
                        AiSearchScope.LOCAL_ONLY, List.of()));

        var context = resolver.resolve(question, profile, AiConversationContext.empty());

        assertThat(context.userProfile().region()).isEqualTo("경기도 수원시");
        assertThat(context.searchProfile().region()).isEqualTo("부산광역시 해운대구");
    }

    @Test
    void doesNotPromoteAmbiguousRegionToLocalResourceSearch() {
        String question = "광주 지원사업 알려줘";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "서울특별시 강남구", "서울특별시", "강남구",
                null, List.of(), List.of(), null);
        var ambiguousRegion = new AiQuestionRegionResolver.RegionResolution(
                AiQuestionRegionResolver.RegionResolution.Status.AMBIGUOUS,
                null,
                null,
                List.of("광주광역시", "경기도 광주시")
        );
        when(regionResolver.resolve(question, profile)).thenReturn(ambiguousRegion);
        when(classifier.analyze(any(), any(), any(), any(), any()))
                .thenReturn(AiQuestionAnalysis.forQuestion(
                question, AiQuestionIntent.NONE, AiSearchScope.REGION_PRIORITY, List.of()));

        var context = resolver.resolve(question, profile, AiConversationContext.empty());

        assertThat(context.searchScope()).isEqualTo(AiSearchScope.REGION_PRIORITY);
    }

    @Test
    void normalizesConflictingListFlagsToSingleSiteResultType() {
        String question = "복지 사이트 추천해줘";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        when(regionResolver.resolve(question, profile))
                .thenReturn(AiQuestionRegionResolver.RegionResolution.notFound());
        when(classifier.analyze(any(), any(), any(), any(), any()))
                .thenReturn(AiQuestionAnalysis.forQuestion(
                                question, AiQuestionIntent.NONE,
                                AiSearchScope.REGION_PRIORITY, List.of())
                        .withSiteListRequest(true)
                        .withResourceListRequest(true));

        var context = resolver.resolve(question, profile, AiConversationContext.empty());

        assertThat(context.resultType()).isEqualTo(AiResultType.SITE_LIST);
        assertThat(context.isSiteListRequest()).isTrue();
        assertThat(context.isResourceListRequest()).isFalse();
    }

    @Test
    void treatsRehabCenterHomepagesAsSiteListEvenWhenIntentIsLocalRehab() {
        String question = "재활센터 홈페이지 5개 알려줘";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        when(regionResolver.resolve(any(String.class), any(AiUserProfile.class)))
                .thenReturn(AiQuestionRegionResolver.RegionResolution.notFound());
        when(classifier.analyze(any(), any(), any(), any(), any()))
                .thenReturn(AiQuestionAnalysis.forQuestion(
                                question, AiQuestionIntent.LOCAL_REHAB_CENTERS,
                                AiSearchScope.LOCAL_ONLY, List.of(), 5)
                        .withSiteListRequest(true));

        var context = resolver.resolve(question, profile, AiConversationContext.empty());

        assertThat(context.resultType()).isEqualTo(AiResultType.SITE_LIST);
    }

    @Test
    void keepsHomepageAvailabilityQuestionAsResourceList() {
        String question = "홈페이지가 있는 재활센터 알려줘";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        when(regionResolver.resolve(any(String.class), any(AiUserProfile.class)))
                .thenReturn(AiQuestionRegionResolver.RegionResolution.notFound());
        when(classifier.analyze(any(), any(), any(), any(), any()))
                .thenReturn(AiQuestionAnalysis.forQuestion(
                                question, AiQuestionIntent.LOCAL_REHAB_CENTERS,
                                AiSearchScope.LOCAL_ONLY, List.of())
                        .withResourceListRequest(true));

        var context = resolver.resolve(question, profile, AiConversationContext.empty());

        assertThat(context.resultType()).isEqualTo(AiResultType.RESOURCE_LIST);
    }

    @Test
    void usesStructuredFilterInsteadOfKeywordOrderForFollowUpCondition() {
        String question = "공립 말고 사립만 알려줘";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        AiResolvedContext previousContext = new AiResolvedContext(
                "특수학교", null, Map.of("설립구분", "공립"), "목록", null);
        AiResolvedContext analyzedContext = new AiResolvedContext(
                null, null, Map.of("설립구분", "사립"), null, null);
        AiConversationContext conversationContext = new AiConversationContext(
                "사용자: 수원 공립 특수학교 알려줘",
                "수원 공립 특수학교 알려줘",
                "공립 특수학교 안내",
                "수원 공립 특수학교 알려줘",
                previousContext, 1L, 1L);
        when(regionResolver.resolve(any(String.class), any(AiUserProfile.class)))
                .thenReturn(AiQuestionRegionResolver.RegionResolution.notFound());
        when(classifier.analyze(any(), any(), any(), any(), any(), any()))
                .thenReturn(AiQuestionAnalysis.forQuestion(
                                question, AiQuestionIntent.NONE,
                                AiSearchScope.REGION_PRIORITY, List.of())
                        .withResolvedContext(analyzedContext)
                        .withConversationContext(true, false));

        var context = resolver.resolve(question, profile, conversationContext);

        assertThat(context.resolvedContext().filters())
                .containsEntry("설립구분", "사립");
    }

    @Test
    void preservesPreviousResultExclusionForAdditionalSiteRequest() {
        String question = "다른 복지 사이트 더 알려줘";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        AiResolvedContext previousContext = new AiResolvedContext(
                "공식 복지 사이트", null, Map.of(), "목록", 5);
        AiConversationContext conversationContext = new AiConversationContext(
                "사용자: 복지 사이트 5개 알려줘",
                "복지 사이트 5개 알려줘",
                "복지 사이트 안내",
                "복지 사이트 5개 알려줘",
                previousContext, 1L, 1L);
        when(regionResolver.resolve(any(String.class), any(AiUserProfile.class)))
                .thenReturn(AiQuestionRegionResolver.RegionResolution.notFound());
        when(classifier.analyze(any(), any(), any(), any(), any(), any()))
                .thenReturn(AiQuestionAnalysis.forQuestion(
                                question, AiQuestionIntent.NONE,
                                AiSearchScope.REGION_PRIORITY, List.of())
                        .withResolvedContext(previousContext)
                        .withSiteListRequest(true)
                        .withConversationContext(true, true));

        var context = resolver.resolve(question, profile, conversationContext);

        assertThat(context.resultType()).isEqualTo(AiResultType.SITE_LIST);
        assertThat(context.followUp()).isTrue();
        assertThat(context.excludePreviousResults()).isTrue();
    }

    @Test
    void preservesPreviousSiteTypeWhenAdditionalRequestIsMisclassified() {
        String question = "아직 안내하지 않은 곳 더 보여줘";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        AiResolvedContext previousContext = new AiResolvedContext(
                "공식 복지 사이트", null, Map.of(), "목록", 5,
                AiResultType.SITE_LIST);
        AiConversationContext conversationContext = new AiConversationContext(
                "사용자: 복지 사이트 알려줘", "복지 사이트 알려줘",
                "복지 사이트 안내", "복지 사이트 알려줘",
                previousContext, 1L, 1L);
        when(regionResolver.resolve(any(String.class), any(AiUserProfile.class)))
                .thenReturn(AiQuestionRegionResolver.RegionResolution.notFound());
        when(classifier.analyze(any(), any(), any(), any(), any(), any()))
                .thenReturn(AiQuestionAnalysis.forQuestion(
                                question, AiQuestionIntent.NONE,
                                AiSearchScope.REGION_PRIORITY, List.of())
                        .withResourceListRequest(true)
                        .withConversationContext(true, true));

        var context = resolver.resolve(question, profile, conversationContext);

        assertThat(context.resultType()).isEqualTo(AiResultType.SITE_LIST);
        assertThat(context.excludePreviousResults()).isTrue();
    }

    @Test
    void enforcesExclusionForExplicitAdditionalPhrasesWhenLlmMissesFollowUp() {
        for (String question : List.of(
                "아직 안내하지 않은 곳 더 보여줘",
                "앞에서 말한 곳 빼고 추천해줘",
                "새로운 기관만 더 찾아줘")) {
            AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
            AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
            AiQuestionContextResolver resolver =
                    new AiQuestionContextResolver(classifier, regionResolver);
            AiUserProfile profile = new AiUserProfile(
                    "경기도 수원시", "경기도", "수원시",
                    null, List.of(), List.of(), null);
            AiResolvedContext previousContext = new AiResolvedContext(
                    "재활센터", null, Map.of(), "목록", 5,
                    AiResultType.RESOURCE_LIST);
            AiConversationContext conversationContext = new AiConversationContext(
                    "사용자: 재활센터 알려줘", "재활센터 알려줘",
                    "재활센터 안내", "재활센터 알려줘",
                    previousContext, 1L, 1L);
            when(regionResolver.resolve(any(String.class), any(AiUserProfile.class)))
                    .thenReturn(AiQuestionRegionResolver.RegionResolution.notFound());
            when(classifier.analyze(any(), any(), any(), any(), any(), any()))
                    .thenReturn(AiQuestionAnalysis.forQuestion(
                            question, AiQuestionIntent.NONE,
                            AiSearchScope.REGION_PRIORITY, List.of()));

            var context = resolver.resolve(question, profile, conversationContext);

            assertThat(context.followUp()).as(question).isTrue();
            assertThat(context.excludePreviousResults()).as(question).isTrue();
            assertThat(context.resultType()).as(question)
                    .isEqualTo(AiResultType.RESOURCE_LIST);
        }
    }

    @Test
    void doesNotTreatMoreDetailedExplanationAsAdditionalListRequest() {
        String question = "신청 방법을 더 자세히 알려줘";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        AiResolvedContext previousContext = new AiResolvedContext(
                "발달재활서비스 바우처", null, Map.of(), "목록", 5,
                AiResultType.RESOURCE_LIST);
        AiConversationContext conversationContext = new AiConversationContext(
                "사용자: 바우처 목록 알려줘", "바우처 목록 알려줘",
                "바우처 안내", "바우처 목록 알려줘",
                previousContext, 1L, 1L);
        when(regionResolver.resolve(any(String.class), any(AiUserProfile.class)))
                .thenReturn(AiQuestionRegionResolver.RegionResolution.notFound());
        when(classifier.analyze(any(), any(), any(), any(), any(), any()))
                .thenReturn(AiQuestionAnalysis.forQuestion(
                        question, AiQuestionIntent.NONE,
                        AiSearchScope.NATIONWIDE, List.of()));

        var context = resolver.resolve(question, profile, conversationContext);

        assertThat(context.excludePreviousResults()).isFalse();
        assertThat(context.resultType()).isEqualTo(AiResultType.DOCUMENT_ANSWER);
    }

    @Test
    void asksForResourceCategoryWhenListTargetIsTooBroad() {
        String question = "장애아동 관련 기관 알려줘";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        when(regionResolver.resolve(any(String.class), any(AiUserProfile.class)))
                .thenReturn(AiQuestionRegionResolver.RegionResolution.notFound());
        when(classifier.analyze(any(), any(), any(), any(), any()))
                .thenReturn(AiQuestionAnalysis.forQuestion(
                                question, AiQuestionIntent.NONE,
                                AiSearchScope.REGION_PRIORITY, List.of())
                        .withResourceListRequest(true));

        var context = resolver.resolve(question, profile, AiConversationContext.empty());

        assertThat(context.resultType()).isEqualTo(AiResultType.RESOURCE_LIST);
        assertThat(context.needsClarification()).isTrue();
        assertThat(context.clarificationQuestion()).contains("어떤 종류의 기관");
    }

    @Test
    void doesNotInheritPreviousRegionWhenResourceCategoryChanges() {
        String question = "재활센터 알려줘";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        AiConversationContext conversationContext = new AiConversationContext(
                "사용자: 부산 특수학교 알려줘",
                "부산 특수학교 알려줘",
                "부산 특수학교 안내",
                "부산 특수학교 알려줘",
                new AiResolvedContext(
                        "특수학교",
                        new AiResolvedContext.RegionContext("부산광역시", null),
                        Map.of(), "목록", null),
                1L, 1L);
        when(regionResolver.resolve(any(String.class), any(AiUserProfile.class)))
                .thenReturn(AiQuestionRegionResolver.RegionResolution.notFound());
        when(classifier.analyze(any(), any(), any(), any(), any(), any()))
                .thenReturn(AiQuestionAnalysis.forQuestion(
                        question, AiQuestionIntent.NONE,
                        AiSearchScope.REGION_PRIORITY, List.of()));

        var context = resolver.resolve(question, profile, conversationContext);

        assertThat(context.followUp()).isFalse();
        assertThat(context.searchProfile().region()).isEqualTo("경기도 수원시");
        assertThat(context.searchProfile().infoSubCategory())
                .isEqualTo(com.bodeum.domain.info.entity.enums.InfoSubCategory.THERAPY_REHAB);
    }

    @Test
    void keepsPreviousRegionForAdditionalResultsInSameResourceCategory() {
        String question = "특수학교 더 알려줘";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        AiResolvedContext previousContext = new AiResolvedContext(
                "특수학교",
                new AiResolvedContext.RegionContext("부산광역시", null),
                Map.of(), "목록", 5);
        AiConversationContext conversationContext = new AiConversationContext(
                "사용자: 부산 특수학교 알려줘",
                "부산 특수학교 알려줘",
                "부산 특수학교 안내",
                "부산 특수학교 알려줘",
                previousContext, 1L, 1L);
        Region busan = Region.create("부산광역시", "해운대구");
        when(regionResolver.resolve(any(String.class), any(AiUserProfile.class)))
                .thenAnswer(invocation -> invocation.<String>getArgument(0).contains("부산")
                        ? AiQuestionRegionResolver.RegionResolution.resolved(busan)
                        : AiQuestionRegionResolver.RegionResolution.notFound());
        when(classifier.analyze(any(), any(), any(), any(), any(), any()))
                .thenReturn(AiQuestionAnalysis.forQuestion(
                                question, AiQuestionIntent.NONE,
                                AiSearchScope.REGION_PRIORITY, List.of())
                        .withResolvedContext(new AiResolvedContext(
                                "특수학교", null, Map.of(), "목록", 5))
                        .withConversationContext(true, true));

        var context = resolver.resolve(question, profile, conversationContext);

        assertThat(context.followUp()).isTrue();
        assertThat(context.excludePreviousResults()).isTrue();
        assertThat(context.searchProfile().region()).isEqualTo("부산광역시 해운대구");
    }

    @Test
    void keepsImmediateRehabListContextForShortAdditionalRequest() {
        String question = "더 알려줘";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        AiResolvedContext previousContext = new AiResolvedContext(
                "재활센터",
                new AiResolvedContext.RegionContext("경기도", "수원시"),
                Map.of(), "목록", 5, AiResultType.RESOURCE_LIST);
        AiConversationContext conversationContext = new AiConversationContext(
                "사용자: 근처 재활센터를 알려줘",
                "근처 재활센터를 알려줘",
                "수원시 재활센터 안내",
                "근처 재활센터를 알려줘",
                previousContext, 1L, 1L);
        Region suwon = Region.create("경기도", "수원시");
        when(regionResolver.resolve(any(String.class), any(AiUserProfile.class)))
                .thenAnswer(invocation -> invocation.<String>getArgument(0).contains("수원")
                        ? AiQuestionRegionResolver.RegionResolution.resolved(suwon)
                        : AiQuestionRegionResolver.RegionResolution.notFound());
        when(classifier.analyze(any(), any(), any(), any(), any(), any()))
                .thenReturn(AiQuestionAnalysis.forQuestion(
                        "수원시 장애인가족지원센터 알려줘",
                        AiQuestionIntent.NONE,
                        AiSearchScope.LOCAL_ONLY,
                        List.of(),
                        5
                ).withResourceListRequest(true));

        var context = resolver.resolve(question, profile, conversationContext);

        assertThat(context.followUp()).isTrue();
        assertThat(context.excludePreviousResults()).isTrue();
        assertThat(context.resultType()).isEqualTo(AiResultType.RESOURCE_LIST);
        assertThat(context.resolvedContext()).isEqualTo(previousContext);
        assertThat(context.searchProfile().infoSubCategory())
                .isEqualTo(com.bodeum.domain.info.entity.enums.InfoSubCategory.THERAPY_REHAB);
        assertThat(context.resolvedQuestion()).contains("재활센터").doesNotContain("가족지원");
    }

    @Test
    void keepsImmediateSiteListContextForShortAdditionalRequest() {
        String question = "더 알려줘";
        AiQuestionIntentClassifier classifier = mock(AiQuestionIntentClassifier.class);
        AiQuestionRegionResolver regionResolver = mock(AiQuestionRegionResolver.class);
        AiQuestionContextResolver resolver =
                new AiQuestionContextResolver(classifier, regionResolver);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        AiResolvedContext previousContext = new AiResolvedContext(
                "장애아동 공식 사이트",
                null,
                Map.of(), "목록", 5, AiResultType.SITE_LIST);
        AiConversationContext conversationContext = new AiConversationContext(
                "사용자: 장애아동 사이트 알려줘",
                "장애아동 사이트 알려줘",
                "공식 사이트 안내",
                "장애아동 사이트 알려줘",
                previousContext, 1L, 1L);
        when(regionResolver.resolve(any(String.class), any(AiUserProfile.class)))
                .thenReturn(AiQuestionRegionResolver.RegionResolution.notFound());
        when(classifier.analyze(any(), any(), any(), any(), any(), any()))
                .thenReturn(AiQuestionAnalysis.forQuestion(
                        "장애아동 관련 기관 알려줘",
                        AiQuestionIntent.NONE,
                        AiSearchScope.REGION_PRIORITY,
                        List.of(),
                        5
                ).withResourceListRequest(true));

        var context = resolver.resolve(question, profile, conversationContext);

        assertThat(context.followUp()).isTrue();
        assertThat(context.excludePreviousResults()).isTrue();
        assertThat(context.resultType()).isEqualTo(AiResultType.SITE_LIST);
        assertThat(context.resolvedContext().resultType()).isEqualTo(AiResultType.SITE_LIST);
        assertThat(context.resolvedQuestion()).contains("사이트").doesNotContain("기관");
    }
}
