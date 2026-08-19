package com.bodeum.domain.ai.service.retrieval;

import com.bodeum.domain.ai.service.validation.AiAnswerEvidenceService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.ai.model.rag.AiSourceKey;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.enums.InfoSubCategory;
import com.bodeum.domain.info.entity.enums.MainCategory;
import com.bodeum.domain.info.repository.InfoItemRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class AiResourceListSearchServiceTest {

    private final InfoItemRepository repository = mock(InfoItemRepository.class);
    private final AiAnswerEvidenceService evidenceService = mock(AiAnswerEvidenceService.class);
    private AiResourceListSearchService service;

    @BeforeEach
    void setUp() {
        service = new AiResourceListSearchService(repository, evidenceService);
        ReflectionTestUtils.setField(service, "defaultResultCount", 5);
        ReflectionTestUtils.setField(service, "maxResultCount", 10);
        ReflectionTestUtils.setField(service, "maxCandidateCount", 30);
        when(evidenceService.documentIdentityKeys(any()))
                .thenAnswer(invocation -> Set.of(
                        "title:" + invocation.<com.bodeum.domain.ai.model.rag.AiReferenceDocument>
                                getArgument(0).title()));
    }

    @Test
    void returnsRequestedLocalResourcesFromMysqlWithoutSimilarityFiltering() {
        List<InfoItem> centers = IntStream.rangeClosed(1, 8)
                .mapToObj(index -> center((long) index, "재활센터 " + index))
                .toList();
        when(repository.findRehabCentersByRegion(
                eq("경기도"), eq("수원시"), eq(InfoSubCategory.THERAPY_REHAB),
                any(Pageable.class))).thenReturn(centers);

        var result = service.retrieve(profile(), AiSearchScope.LOCAL_ONLY, 5,
                Set.of(), Set.of());

        assertThat(result).extracting(document -> document.sourceId())
                .containsExactly(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    void excludesPreviouslyAnsweredResourcesBeforeApplyingRequestedLimit() {
        List<InfoItem> centers = IntStream.rangeClosed(1, 8)
                .mapToObj(index -> center((long) index, "재활센터 " + index))
                .toList();
        when(repository.findRehabCentersByRegion(
                eq("경기도"), eq("수원시"), eq(InfoSubCategory.THERAPY_REHAB),
                any(Pageable.class))).thenReturn(centers);
        when(repository.findRehabCentersByRegionExcludingIds(
                eq("경기도"), eq("수원시"), eq(InfoSubCategory.THERAPY_REHAB),
                any(), any(Pageable.class))).thenReturn(centers);

        var result = service.retrieve(profile(), AiSearchScope.LOCAL_ONLY, 5,
                Set.of(
                        new AiSourceKey(AiResponseSourceType.INFO, 1L),
                        new AiSourceKey(AiResponseSourceType.INFO, 2L)),
                Set.of("title:재활센터 3"));

        assertThat(result).extracting(document -> document.sourceId())
                .containsExactly(4L, 5L, 6L, 7L, 8L);
    }

    @Test
    void clampsNonPositiveRequestedCountToOne() {
        List<InfoItem> centers = List.of(
                center(1L, "재활센터 1"),
                center(2L, "재활센터 2")
        );
        when(repository.findRehabCentersByRegion(
                eq("경기도"), eq("수원시"), eq(InfoSubCategory.THERAPY_REHAB),
                any(Pageable.class))).thenReturn(centers);

        var zeroResult = service.retrieve(profile(), AiSearchScope.LOCAL_ONLY, 0,
                Set.of(), Set.of());
        var negativeResult = service.retrieve(profile(), AiSearchScope.LOCAL_ONLY, -3,
                Set.of(), Set.of());

        assertThat(zeroResult).extracting(document -> document.sourceId())
                .containsExactly(1L);
        assertThat(negativeResult).extracting(document -> document.sourceId())
                .containsExactly(1L);
    }

    @Test
    void expandsCandidateWindowByPreviouslyExcludedResourceCount() {
        when(repository.findRehabCentersByRegionExcludingIds(
                eq("경기도"), eq("수원시"), eq(InfoSubCategory.THERAPY_REHAB),
                any(), any(Pageable.class))).thenReturn(List.of());
        Set<AiSourceKey> excludedSources = IntStream.rangeClosed(1, 25)
                .mapToObj(id -> new AiSourceKey(AiResponseSourceType.INFO, (long) id))
                .collect(java.util.stream.Collectors.toSet());

        service.retrieve(profile(), AiSearchScope.LOCAL_ONLY, 5,
                excludedSources, Set.of());

        org.mockito.ArgumentCaptor<Pageable> pageCaptor =
                org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findRehabCentersByRegionExcludingIds(
                eq("경기도"), eq("수원시"), eq(InfoSubCategory.THERAPY_REHAB),
                any(), pageCaptor.capture());
        assertThat(pageCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void addsSourceAndIdentityExclusionsWhenExpandingCandidateWindow() {
        ReflectionTestUtils.setField(service, "maxCandidateCount", 100);
        when(repository.findRehabCentersByRegionExcludingIds(
                eq("경기도"), eq("수원시"), eq(InfoSubCategory.THERAPY_REHAB),
                any(), any(Pageable.class))).thenReturn(List.of());
        Set<AiSourceKey> excludedSources = IntStream.rangeClosed(1, 25)
                .mapToObj(id -> new AiSourceKey(AiResponseSourceType.INFO, (long) id))
                .collect(java.util.stream.Collectors.toSet());
        Set<String> excludedIdentities = IntStream.rangeClosed(1, 25)
                .mapToObj(id -> "title:재활센터 " + id)
                .collect(java.util.stream.Collectors.toSet());

        service.retrieve(profile(), AiSearchScope.LOCAL_ONLY, 5,
                excludedSources, excludedIdentities);

        org.mockito.ArgumentCaptor<Pageable> pageCaptor =
                org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findRehabCentersByRegionExcludingIds(
                eq("경기도"), eq("수원시"), eq(InfoSubCategory.THERAPY_REHAB),
                any(), pageCaptor.capture());
        assertThat(pageCaptor.getValue().getPageSize()).isEqualTo(30);
    }

    @Test
    void excludesPreviousInfoIdsInDatabaseBeforeApplyingCandidateLimit() {
        ReflectionTestUtils.setField(service, "maxCandidateCount", 30);
        when(repository.findRehabCentersByRegionExcludingIds(
                eq("경기도"), eq("수원시"), eq(InfoSubCategory.THERAPY_REHAB),
                any(), any(Pageable.class))).thenReturn(List.of(
                        center(31L, "재활센터 31"),
                        center(32L, "재활센터 32")
                ));
        Set<AiSourceKey> excludedSources = IntStream.rangeClosed(1, 30)
                .mapToObj(id -> new AiSourceKey(AiResponseSourceType.INFO, (long) id))
                .collect(java.util.stream.Collectors.toSet());

        var result = service.retrieve(profile(), AiSearchScope.LOCAL_ONLY, 2,
                excludedSources, Set.of());

        assertThat(result).extracting(AiReferenceDocument::sourceId)
                .containsExactly(31L, 32L);
        verify(repository).findRehabCentersByRegionExcludingIds(
                eq("경기도"), eq("수원시"), eq(InfoSubCategory.THERAPY_REHAB),
                eq(IntStream.rangeClosed(1, 30).mapToObj(Long::valueOf)
                        .collect(java.util.stream.Collectors.toSet())),
                any(Pageable.class));
    }

    private AiUserProfile profile() {
        return new AiUserProfile(
                "경기도 수원시", "경기도", "수원시", null,
                List.of(), List.of(), null).withInfoSubCategory(
                InfoSubCategory.THERAPY_REHAB);
    }

    private InfoItem center(Long id, String name) {
        InfoCategory category = new InfoCategory(
                6L, MainCategory.INSTITUTION, "기관",
                InfoSubCategory.THERAPY_REHAB, "치료·재활기관");
        InfoItem item = InfoItem.builder()
                .externalId("center-" + id)
                .infoCategory(category)
                .name(name)
                .address("경기도 수원시")
                .sido("경기도")
                .sigungu("수원시")
                .syncedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }
}
