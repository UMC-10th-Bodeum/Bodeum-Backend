package com.bodeum.domain.ai.service.response;

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
import com.bodeum.domain.ai.model.question.AiCuratedAnswerType;
import com.bodeum.domain.ai.model.question.AiStarterQuestionCatalog;
import com.bodeum.domain.ai.infrastructure.external.AiExternalDocumentPersistenceService;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.repository.AiExternalSourceRepository;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.repository.InfoItemRepository;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

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
    void returnsReviewedDiagnosisFirstStepsAnswerWithOfficialSources() {
        List<AiExternalSource> sources = List.of(
                source("보건복지부", "https://www.mohw.go.kr/"),
                source("사회서비스 전자바우처", "https://www.socialservice.or.kr:444/"),
                source("복지로", "https://www.bokjiro.go.kr/"),
                source("보건복지상담센터", "https://www.129.go.kr/")
        );
        when(externalSourceRepository.findAllBySourceTypeAndActiveTrue(
                AiExternalSourceType.WEBSITE
        )).thenReturn(sources);
        List<AiExternalDocument> documents = List.of(
                document(1L, "https://www.mohw.go.kr/board.es"),
                document(2L, "https://www.socialservice.or.kr:444/user/htmlEditor/view2.do"),
                document(3L, "https://www.bokjiro.go.kr/ssis-tbu/twatzzza/intgSearch/"),
                document(4L, "https://www.129.go.kr/")
        );
        when(externalDocumentPersistenceService.saveAll(any())).thenReturn(documents);

        var result = router.route(
                AiCuratedAnswerType.DIAGNOSIS_FIRST_STEPS,
                profile("경기도 수원시")
        ).orElseThrow();

        assertThat(result.hasEvidence()).isTrue();
        assertThat(result.content()).contains(
                "진단 이후 챙기면 좋은 순서",
                "① 장애인 등록",
                "② 발달재활서비스 바우처 신청",
                "③ 의료비 지원 대상 확인",
                "④ 지역 기관 연결"
        );
        assertThat(result.sources()).hasSize(4);
        assertThat(result.sources())
                .allMatch(source -> source.sourceType() == AiResponseSourceType.SITE);
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
                AiCuratedAnswerType.WELFARE_SITES,
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
    void limitsFixedWelfareSitesToRequestedCount() {
        List<AiExternalSource> sources = List.of(
                source("복지로", "https://www.bokjiro.go.kr"),
                source("발달장애인지원포털", "https://www.broso.or.kr/mainPage.do"),
                source("사회서비스 전자바우처", "https://www.socialservice.or.kr")
        );
        when(externalSourceRepository.findAllBySourceTypeAndActiveTrue(
                AiExternalSourceType.WEBSITE
        )).thenReturn(sources);
        List<AiExternalDocument> documents = List.of(
                document(1L, sources.get(0).getBaseUrl()),
                document(2L, sources.get(1).getBaseUrl()),
                document(3L, sources.get(2).getBaseUrl())
        );
        when(externalDocumentPersistenceService.saveAll(any())).thenReturn(documents);

        var result = router.route(
                AiCuratedAnswerType.WELFARE_SITES,
                profile("경기도 성남시"),
                3
        ).orElseThrow();

        assertThat(result.sources()).hasSize(3);
        assertThat(result.content())
                .contains("공식 복지 사이트 3개를 추천드리겠습니다")
                .doesNotContain("정부24");
    }

    @Test
    void preservesCuratedSitesWhenRequestedCountExceedsCuratedList() {
        assertThat(router.route(
                AiCuratedAnswerType.AUTISM_INFO_SITES,
                profile("경기도 성남시"),
                10
        )).isPresent();
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

        AiCuratedAnswerType type = AiStarterQuestionCatalog.findAnswerType(
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
                AiCuratedAnswerType.WELFARE_SITES,
                profile("경기도 수원시")
        ).orElseThrow();

        assertThat(result.hasEvidence()).isFalse();
        verify(externalDocumentPersistenceService, never()).saveAll(any());
    }

    @Test
    void returnsReviewedAutismInformationSitesWithoutAddingStarterChip() {
        List<AiExternalSource> sources = List.of(
                source("중앙장애아동·발달장애인지원센터", "https://www.broso.or.kr/"),
                source("한국자폐인사랑협회", "https://www.autismkorea.kr/"),
                source("국립특수교육원 온맘", "https://www.nise.go.kr/")
        );
        when(externalSourceRepository.findAllBySourceTypeAndActiveTrue(
                AiExternalSourceType.WEBSITE
        )).thenReturn(sources);
        List<AiExternalDocument> documents = List.of(
                document(1L, "https://www.broso.or.kr/mainPage.do"),
                document(2L, "https://www.autismkorea.kr/main.php"),
                document(3L, "https://www.nise.go.kr/onmam/front/index.do")
        );
        when(externalDocumentPersistenceService.saveAll(any())).thenReturn(documents);

        AiCuratedAnswerType type = AiStarterQuestionCatalog.findAnswerType(
                "자폐스펙트럼 정보 사이트 알려주세요"
        ).orElseThrow();
        var result = router.route(type, profile(null)).orElseThrow();

        assertThat(type).isEqualTo(AiCuratedAnswerType.AUTISM_INFO_SITES);
        assertThat(AiStarterQuestionCatalog.isVisible(type)).isFalse();
        assertThat(result.hasEvidence()).isTrue();
        assertThat(result.sources()).hasSize(3);
        assertThat(result.content()).contains(
                "자폐스펙트럼 관련 정보를 얻을 수 있는 사이트",
                "중앙장애아동·발달장애인지원센터",
                "한국자폐인사랑협회",
                "국립특수교육원 온맘",
                "자폐성장애 등록 기준"
        );
        assertThat(result.content()).doesNotContain("보건복지부 발달장애인지원포털");
    }

    @Test
    void returnsReviewedChildMedicalSupportAnswerWithOfficialSources() {
        List<AiExternalSource> sources = List.of(
                source("보건복지부", "https://www.mohw.go.kr/"),
                source("국민건강보험공단", "https://www.nhis.or.kr/")
        );
        when(externalSourceRepository.findAllBySourceTypeAndActiveTrue(
                AiExternalSourceType.WEBSITE
        )).thenReturn(sources);
        List<AiExternalDocument> documents = List.of(
                document(1L, "https://www.mohw.go.kr/menu.es?mid=a10710060700"),
                document(2L, "https://www.nhis.or.kr/nhis/minwon/minwonServiceBoard.do"),
                document(3L, "https://www.nhis.or.kr/nhis/minwon/minwonServiceBoard.do")
        );
        when(externalDocumentPersistenceService.saveAll(any())).thenReturn(documents);

        var result = router.route(
                AiCuratedAnswerType.CHILD_MEDICAL_SUPPORT,
                profile("경기도 수원시")
        ).orElseThrow();

        assertThat(result.hasEvidence()).isTrue();
        assertThat(result.content()).contains(
                "장애아동이 받을 수 있는 의료비 지원",
                "장애인 의료비 지원",
                "본인부담액상한제",
                "재난적의료비 지원사업",
                "2026년 기준, 기준 중위소득 100% 이하"
        );
        assertThat(result.sources()).hasSize(3);
        assertThat(result.sources())
                .allMatch(source -> source.sourceType() == AiResponseSourceType.SITE);
    }

    @Test
    void returnsNoEvidenceWhenMedicalSupportOfficialSourceIsMissing() {
        AiExternalSource mohw = source("보건복지부", "https://www.mohw.go.kr/");
        when(externalSourceRepository.findAllBySourceTypeAndActiveTrue(
                AiExternalSourceType.WEBSITE
        )).thenReturn(List.of(mohw));

        var result = router.route(
                AiCuratedAnswerType.CHILD_MEDICAL_SUPPORT,
                profile(null)
        ).orElseThrow();

        assertThat(result.hasEvidence()).isFalse();
        verify(externalDocumentPersistenceService, never()).saveAll(any());
    }

    @Test
    void returnsReviewedVoucherApplicationAnswerWithOfficialSources() {
        List<AiExternalSource> sources = List.of(
                source("보건복지부", "https://www.mohw.go.kr/"),
                source("사회서비스 전자바우처", "https://www.socialservice.or.kr:444/")
        );
        when(externalSourceRepository.findAllBySourceTypeAndActiveTrue(
                AiExternalSourceType.WEBSITE
        )).thenReturn(sources);
        List<AiExternalDocument> documents = List.of(
                document(1L, "https://www.mohw.go.kr/menu.es?mid=a10710060600"),
                document(2L, "https://www.socialservice.or.kr:444/user/htmlEditor/view2.do")
        );
        when(externalDocumentPersistenceService.saveAll(any())).thenReturn(documents);

        var result = router.route(
                AiCuratedAnswerType.VOUCHER_APPLICATION,
                profile("경기도 수원시")
        ).orElseThrow();

        assertThat(result.hasEvidence()).isTrue();
        assertThat(result.content()).contains(
                "발달재활서비스 바우처 신청 안내",
                "2026년 기준",
                "기준 중위소득 180% 이하",
                "복지로(bokjiro.go.kr) 온라인 신청",
                "사회서비스 전자바우처(socialservice.or.kr)",
                "경기도 수원시 내 지정기관"
        );
        assertThat(result.sources()).hasSize(2);
        assertThat(result.sources())
                .allMatch(source -> source.sourceType() == AiResponseSourceType.SITE);
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
        when(center.getIntroduction()).thenReturn(null);
        when(infoItemRepository.findRehabCentersByRegion(
                eq("경기도"),
                eq("수원시"),
                eq(com.bodeum.domain.info.entity.enums.InfoSubCategory.THERAPY_REHAB),
                any(Pageable.class)
        )).thenReturn(List.of(center));

        var result = router.route(
                AiCuratedAnswerType.LOCAL_REHAB_CENTERS,
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
                .doesNotContain(
                        "조회 10", "저장 5", "후기 2", "활동 점수", "확인 필요");
        assertThat(result.sources().getFirst().sourceId()).isEqualTo(10L);
    }

    @Test
    void usesRequestedCountForLocalRehabCenterLookup() {
        when(infoItemRepository.findRehabCentersByRegion(
                eq("경기도"),
                eq("수원시"),
                eq(com.bodeum.domain.info.entity.enums.InfoSubCategory.THERAPY_REHAB),
                any(Pageable.class)
        )).thenReturn(List.of());

        router.route(
                AiCuratedAnswerType.LOCAL_REHAB_CENTERS,
                profile("경기도 수원시"),
                10
        ).orElseThrow();

        ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(infoItemRepository).findRehabCentersByRegion(
                eq("경기도"),
                eq("수원시"),
                eq(com.bodeum.domain.info.entity.enums.InfoSubCategory.THERAPY_REHAB),
                pageCaptor.capture()
        );
        assertThat(pageCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void usesCircledNumbersForAllSupportedRehabCenterItems() {
        assertThat(IntStream.range(0, 10)
                .mapToObj(index -> ReflectionTestUtils.<String>invokeMethod(
                        router, "centerNumber", index))
                .toList())
                .containsExactly("①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨", "⑩");
    }

    @Test
    void returnsScopedNoEvidenceMessageWhenLocalRehabCenterIsMissing() {
        when(infoItemRepository.findRehabCentersByRegion(
                eq("경기도"),
                eq("수원시"),
                eq(com.bodeum.domain.info.entity.enums.InfoSubCategory.THERAPY_REHAB),
                any(Pageable.class)
        )).thenReturn(List.of());

        var result = router.route(
                AiCuratedAnswerType.LOCAL_REHAB_CENTERS,
                profile("경기도 수원시"),
                10
        ).orElseThrow();

        assertThat(result.hasNoEvidenceMessage()).isTrue();
        assertThat(result.content()).isEqualTo(
                "현재 보듬에서 확인 가능한 경기도 수원시 재활센터를 찾지 못했습니다.");
    }

    @Test
    void doesNotDescribePageSizeAsTotalRehabCenterCount() {
        String exactRequestedCount = ReflectionTestUtils.invokeMethod(
                router, "localRehabAnswerPrefix", profile("경기도 수원시"), 5, 5);
        String fewerThanRequested = ReflectionTestUtils.invokeMethod(
                router, "localRehabAnswerPrefix", profile("경기도 수원시"), 5, 3);
        String overMaximum = ReflectionTestUtils.invokeMethod(
                router, "localRehabAnswerPrefix", profile("경기도 수원시"), 100, 10);

        assertThat(exactRequestedCount)
                .isEqualTo("요청하신 5곳에 맞춰 현재 보듬에서 확인 가능한 "
                        + "경기도 수원시 재활센터 5곳을 안내드립니다.\n")
                .doesNotContain("확인 가능한 재활센터는");
        assertThat(fewerThanRequested)
                .isEqualTo("요청하신 5곳 중 현재 보듬에서 확인 가능한 경기도 수원시 "
                        + "재활센터 3곳을 안내드립니다.\n");
        assertThat(overMaximum)
                .isEqualTo("한 번에 최대 10곳까지 안내할 수 있어, 현재 보듬에서 확인 가능한 "
                        + "경기도 수원시 재활센터 10곳을 안내드립니다.\n")
                .doesNotContain("요청하신 100곳 중");
    }

    @Test
    void asksForRegionWhenActivityRegionIsMissing() {
        var result = router.route(
                AiCuratedAnswerType.LOCAL_REHAB_CENTERS,
                profile(null)
        ).orElseThrow();

        assertThat(result.isRegionRequired()).isTrue();
        assertThat(result.content()).contains("시·도와 시·군·구를 알려주세요");
        assertThat(result.sources()).isEmpty();
        verify(infoItemRepository, never())
                .findRehabCentersByRegion(any(), any(), any(), any());
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
        return new AiUserProfile(
                region,
                region == null ? null : "경기도",
                region == null ? null : "수원시",
                null,
                List.of(),
                List.of(),
                null
        );
    }
}
