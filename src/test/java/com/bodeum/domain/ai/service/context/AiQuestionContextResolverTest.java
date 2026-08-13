package com.bodeum.domain.ai.service.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.enums.AiQuestionIntent;
import com.bodeum.domain.ai.enums.AiSearchScope;
import com.bodeum.domain.ai.model.context.AiConversationContext;
import com.bodeum.domain.ai.model.rag.AiQuestionAnalysis;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.service.port.AiQuestionIntentClassifier;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiQuestionContextResolverTest {

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
        when(classifier.analyze(question)).thenReturn(AiQuestionAnalysis.forQuestion(
                question, AiQuestionIntent.NONE, AiSearchScope.GENERAL, List.of()));

        var context = resolver.resolve(question, profile, AiConversationContext.empty());

        assertThat(context.searchScope()).isEqualTo(AiSearchScope.GENERAL);
    }
}
