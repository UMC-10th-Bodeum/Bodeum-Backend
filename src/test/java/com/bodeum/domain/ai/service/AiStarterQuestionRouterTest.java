package com.bodeum.domain.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.entity.AiExternalDocument;
import com.bodeum.domain.ai.entity.AiExternalSource;
import com.bodeum.domain.ai.enums.AiExternalSourceType;
import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.enums.AiStarterQuestionType;
import com.bodeum.domain.ai.infrastructure.external.AiExternalDocumentPersistenceService;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.repository.AiExternalSourceRepository;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.repository.InfoItemRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AiStarterQuestionRouterTest {

    @Mock
    private AiExternalSourceRepository externalSourceRepository;
    @Mock
    private AiExternalDocumentPersistenceService externalDocumentPersistenceService;
    @Mock
    private InfoItemRepository infoItemRepository;

    private AiStarterQuestionRouter router;

    @BeforeEach
    void setUp() {
        router = new AiStarterQuestionRouter(
                externalSourceRepository,
                externalDocumentPersistenceService,
                infoItemRepository
        );
    }

    @Test
    void usesGeneralRagUntilDiagnosisGuideIsReviewed() {
        var result = router.route(
                AiStarterQuestionType.DIAGNOSIS_FIRST_STEPS,
                profile("경기도 수원시")
        );

        assertThat(result).isEmpty();
    }

    @Test
    void returnsFiveRegisteredWelfareSitesWithoutCallingOpenAi() {
        List<AiExternalSource> sources = List.of(
                source("복지로", "https://www.bokjiro.go.kr"),
                source("발달장애인지원포털", "https://www.broso.or.kr/mainPage.do"),
                source("사회서비스 전자바우처", "https://www.socialservice.or.kr"),
                source("경기도 장애인가족지원센터", "http://ggdf.co.kr"),
                source("정부24", "https://www.gov.kr")
        );
        when(externalSourceRepository.findAllBySourceTypeAndActiveTrue(
                AiExternalSourceType.WEBSITE
        )).thenReturn(sources);
        List<AiExternalDocument> documents = java.util.stream.LongStream
                .rangeClosed(1, 5)
                .mapToObj(id -> document(id, sources.get((int) id - 1).getBaseUrl()))
                .toList();
        when(externalDocumentPersistenceService.saveAll(any())).thenReturn(documents);

        var result = router.route(
                AiStarterQuestionType.WELFARE_SITES,
                profile("경기도 수원시")
        ).orElseThrow();

        assertThat(result.hasEvidence()).isTrue();
        assertThat(result.sources()).hasSize(5);
        assertThat(result.sources())
                .allMatch(source -> source.sourceType() == AiResponseSourceType.SITE);
        assertThat(result.content()).contains(
                "공식 복지 사이트 5개를 추천드리겠습니다",
                "복지로",
                "정부24",
                "보듬에서도 이 출처들을 기반으로 최신 정보를 정리"
        );
    }

    @Test
    void recognizesPoliteWelfareSiteQuestionTypedByUser() {
        List<AiExternalSource> sources = List.of(
                source("복지로", "https://www.bokjiro.go.kr"),
                source("발달장애인지원포털", "https://www.broso.or.kr/mainPage.do"),
                source("사회서비스 전자바우처", "https://www.socialservice.or.kr"),
                source("경기도 장애인가족지원센터", "http://ggdf.co.kr"),
                source("정부24", "https://www.gov.kr")
        );
        when(externalSourceRepository.findAllBySourceTypeAndActiveTrue(
                AiExternalSourceType.WEBSITE
        )).thenReturn(sources);
        List<AiExternalDocument> documents = java.util.stream.LongStream
                .rangeClosed(1, 5)
                .mapToObj(id -> document(
                        id,
                        sources.get((int) id - 1).getBaseUrl()
                ))
                .toList();
        when(externalDocumentPersistenceService.saveAll(any())).thenReturn(documents);

        AiStarterQuestionType type = AiStarterQuestionType.fromQuestion(
                "참고하면 좋을 복지사이트 알려주세요"
        ).orElseThrow();
        var result = router.route(type, profile(null));

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().hasEvidence()).isTrue();
    }

    @Test
    void returnsNoEvidenceWhenAnyRequiredWelfareSiteIsMissing() {
        AiExternalSource bokjiro = source("복지로", "https://www.bokjiro.go.kr");
        when(externalSourceRepository.findAllBySourceTypeAndActiveTrue(
                AiExternalSourceType.WEBSITE
        )).thenReturn(List.of(bokjiro));

        var result = router.route(
                AiStarterQuestionType.WELFARE_SITES,
                profile("경기도 수원시")
        ).orElseThrow();

        assertThat(result.hasEvidence()).isFalse();
        verify(externalDocumentPersistenceService, never()).saveAll(any());
    }

    @Test
    void returnsRehabCentersInUserActivityRegion() {
        InfoCategory category = org.mockito.Mockito.mock(InfoCategory.class);
        when(category.getSubCategoryKo()).thenReturn("치료·재활 기관");
        InfoItem center = org.mockito.Mockito.mock(InfoItem.class);
        when(center.getId()).thenReturn(10L);
        when(center.getInfoCategory()).thenReturn(category);
        when(center.getName()).thenReturn("수원 언어재활센터");
        when(center.getAddress()).thenReturn("경기도 수원시 팔달구");
        when(center.getSido()).thenReturn("경기도");
        when(center.getSigungu()).thenReturn("수원시");
        when(center.getPhone()).thenReturn("031-111-1111");
        when(infoItemRepository.findRehabCentersByRegion(
                eq("경기도"),
                eq("수원시"),
                any(Pageable.class)
        )).thenReturn(List.of(center));

        var result = router.route(
                AiStarterQuestionType.LOCAL_REHAB_CENTERS,
                profile("경기도 수원시")
        ).orElseThrow();

        assertThat(result.hasEvidence()).isTrue();
        assertThat(result.content())
                .contains(
                        "경기도 수원시",
                        "기관의 우수성을 판단한 결과는 아닙니다",
                        "① 수원 언어재활센터",
                        "방문 전 꼭 전화로 확인"
                )
                .doesNotContain("조회 10", "저장 5", "후기 2", "활동 점수");
        assertThat(result.sources().getFirst().sourceId()).isEqualTo(10L);
    }

    @Test
    void asksForRegionWhenActivityRegionIsMissing() {
        var result = router.route(
                AiStarterQuestionType.LOCAL_REHAB_CENTERS,
                profile(null)
        ).orElseThrow();

        assertThat(result.isRegionRequired()).isTrue();
        assertThat(result.content()).contains("시·도와 시·군·구를 알려주세요");
        assertThat(result.sources()).isEmpty();
        verify(infoItemRepository, never())
                .findRehabCentersByRegion(any(), any(), any());
    }

    private AiExternalSource source(String name, String baseUrl) {
        AiExternalSource source = org.mockito.Mockito.mock(AiExternalSource.class);
        org.mockito.Mockito.lenient().when(source.getName()).thenReturn(name);
        org.mockito.Mockito.lenient().when(source.getBaseUrl()).thenReturn(baseUrl);
        org.mockito.Mockito.lenient().when(source.getDescription())
                .thenReturn(name + " 공식 안내");
        return source;
    }

    private AiExternalDocument document(Long id, String url) {
        AiExternalDocument document = org.mockito.Mockito.mock(AiExternalDocument.class);
        when(document.getId()).thenReturn(id);
        when(document.getSourceUrl()).thenReturn(url);
        return document;
    }

    private AiUserProfile profile(String region) {
        return new AiUserProfile(region, null, List.of(), List.of(), null);
    }
}
