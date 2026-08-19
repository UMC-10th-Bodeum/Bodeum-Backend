package com.bodeum.domain.ai.service.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.model.question.AiCuratedAnswerType;
import com.bodeum.domain.ai.model.question.AiResultType;
import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.region.repository.RegionRepository;
import com.bodeum.domain.region.entity.Region;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class AiStarterQuestionContextResolverTest {

    @Test
    void storesStructuredListContextForRelativeRehabCenterQuestion() {
        AiStarterQuestionContextResolver resolver = new AiStarterQuestionContextResolver(
                mock(AiMessageRepository.class), mock(RegionRepository.class));
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);

        var context = resolver.resolve(
                1L, "근처 재활센터를 알려줘", profile).orElseThrow();

        assertThat(context.curatedAnswerType())
                .contains(AiCuratedAnswerType.LOCAL_REHAB_CENTERS);
        assertThat(context.searchScope()).isEqualTo(AiSearchScope.LOCAL_ONLY);
        assertThat(context.resultType()).isEqualTo(AiResultType.RESOURCE_LIST);
        assertThat(context.resolvedContext().topic()).isEqualTo("재활센터");
        assertThat(context.resolvedContext().requestedInformation()).isEqualTo("목록");
        assertThat(context.resolvedContext().resultType())
                .isEqualTo(AiResultType.RESOURCE_LIST);
        assertThat(context.resolvedContext().region().displayName())
                .isEqualTo("경기도 수원시");
    }

    @Test
    void routesRegionalWelfareSiteQuestionToCuratedAnswerWithRequestedCount() {
        AiMessageRepository messageRepository = mock(AiMessageRepository.class);
        RegionRepository regionRepository = mock(RegionRepository.class);
        Region suwon = Region.create("경기도", "수원시");
        when(regionRepository.findMentionedInQuestion(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(suwon));
        AiStarterQuestionContextResolver resolver = new AiStarterQuestionContextResolver(
                messageRepository, regionRepository);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);

        var context = resolver.resolve(
                1L, "수원시 복지사이트 3개를 알려줘", profile).orElseThrow();

        assertThat(context.curatedAnswerType())
                .contains(AiCuratedAnswerType.WELFARE_SITES);
        assertThat(context.resultType()).isEqualTo(AiResultType.SITE_LIST);
        assertThat(context.requestedResultCount()).isEqualTo(3);
        assertThat(context.resolvedContext().requestedResultCount()).isEqualTo(3);
        assertThat(context.resolvedContext().resultType()).isEqualTo(AiResultType.SITE_LIST);
        assertThat(context.resolvedContext().region()).isNull();
    }

    @Test
    void recognizesNaturalAutismInformationSiteExpression() {
        AiStarterQuestionContextResolver resolver = new AiStarterQuestionContextResolver(
                mock(AiMessageRepository.class), mock(RegionRepository.class));
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);

        var context = resolver.resolve(
                1L, "자폐아 부모가 참고할 사이트가 있을까", profile).orElseThrow();

        assertThat(context.curatedAnswerType())
                .contains(AiCuratedAnswerType.AUTISM_INFO_SITES);
        assertThat(context.resultType()).isEqualTo(AiResultType.SITE_LIST);
    }
}
