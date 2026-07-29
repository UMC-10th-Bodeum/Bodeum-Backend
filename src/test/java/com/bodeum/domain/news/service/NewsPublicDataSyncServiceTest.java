package com.bodeum.domain.news.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.bodeum.domain.news.collector.NewsCandidate;
import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.entity.NewsCategory;
import com.bodeum.domain.news.entity.NewsSource;
import com.bodeum.domain.news.entity.NewsSourceType;
import com.bodeum.domain.news.entity.NewsType;
import com.bodeum.domain.news.entity.RecruitmentStatus;
import com.bodeum.domain.news.infrastructure.openapi.PublicDataNewsCollector;
import com.bodeum.domain.news.repository.NewsCategoryRepository;
import com.bodeum.domain.news.repository.NewsRepository;
import com.bodeum.domain.news.repository.NewsSourceRepository;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.region.repository.RegionRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewsPublicDataSyncServiceTest {

    @Mock
    private PublicDataNewsCollector collector;

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private NewsCategoryRepository newsCategoryRepository;

    @Mock
    private NewsSourceRepository newsSourceRepository;

    @Mock
    private RegionRepository regionRepository;

    private NewsPublicDataSyncService syncService;

    @BeforeEach
    void setUp() {
        syncService = new NewsPublicDataSyncService(
                List.of(collector),
                newsRepository,
                newsCategoryRepository,
                newsSourceRepository,
                regionRepository
        );
    }

    @Test
    void syncLoadsExistingNewsOncePerSource() {
        NewsSource source = mock(NewsSource.class);
        NewsCategory category = mock(NewsCategory.class);
        News existingNews = mock(News.class);
        NewsCandidate existingCandidate = candidate("existing-item", "기존 소식");
        NewsCandidate newCandidate = candidate("new-item", "신규 소식");
        List<String> externalItemIds = List.of("existing-item", "new-item");

        given(regionRepository.findAllByOrderByRegionLevel1AscRegionLevel2Asc())
                .willReturn(List.of());
        given(collector.getSourceType())
                .willReturn(NewsSourceType.PUBLIC_API);
        given(collector.sourceName())
                .willReturn("테스트 공공데이터");
        given(newsSourceRepository.findBySourceTypeAndName(
                NewsSourceType.PUBLIC_API,
                "테스트 공공데이터"
        )).willReturn(Optional.of(source));
        given(collector.collect(source))
                .willReturn(List.of(existingCandidate, newCandidate));
        given(newsRepository.findAllByNewsSourceAndExternalItemIdIn(
                source,
                externalItemIds
        )).willReturn(List.of(existingNews));
        given(existingNews.getExternalItemId())
                .willReturn("existing-item");
        given(newsCategoryRepository.findByNewsTypeAndName(
                NewsType.LOCAL,
                "SUPPORT_SERVICE"
        )).willReturn(Optional.of(category));
        given(newsRepository.save(any(News.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        NewsPublicDataSyncService.NewsSyncResult result = syncService.sync();

        assertThat(result.fetched()).isEqualTo(2);
        assertThat(result.created()).isEqualTo(1);
        assertThat(result.updated()).isEqualTo(1);
        then(newsRepository).should(times(1))
                .findAllByNewsSourceAndExternalItemIdIn(source, externalItemIds);
        then(existingNews).should()
                .updateCollectedData(category, source, null, existingCandidate);
        then(newsRepository).should(times(1)).save(any(News.class));
    }

    @Test
    void syncMapsSuwonGuriAndSeongnamToExistingRegionIds() {
        NewsSource source = mock(NewsSource.class);
        NewsCategory category = mock(NewsCategory.class);
        Region suwon = region(1L, "경기도 수원시");
        Region guri = region(2L, "경기도 구리시");
        Region seongnam = region(3L, "경기도 성남시");
        List<NewsCandidate> candidates = List.of(
                candidate("suwon-item", "수원 소식", "경기도 수원시"),
                candidate("guri-item", "구리 소식", "경기도 구리시"),
                candidate("seongnam-item", "성남 소식", "경기도 성남시")
        );
        List<String> externalItemIds = candidates.stream()
                .map(NewsCandidate::externalItemId)
                .toList();
        List<News> savedNews = new ArrayList<>();

        given(regionRepository.findAllByOrderByRegionLevel1AscRegionLevel2Asc())
                .willReturn(List.of(suwon, guri, seongnam));
        given(collector.getSourceType()).willReturn(NewsSourceType.PUBLIC_API);
        given(collector.sourceName()).willReturn("테스트 공공데이터");
        given(newsSourceRepository.findBySourceTypeAndName(
                NewsSourceType.PUBLIC_API,
                "테스트 공공데이터"
        )).willReturn(Optional.of(source));
        given(collector.collect(source)).willReturn(candidates);
        given(newsRepository.findAllByNewsSourceAndExternalItemIdIn(source, externalItemIds))
                .willReturn(List.of());
        given(newsCategoryRepository.findByNewsTypeAndName(
                NewsType.LOCAL,
                "SUPPORT_SERVICE"
        )).willReturn(Optional.of(category));
        given(newsRepository.save(any(News.class))).willAnswer(invocation -> {
            News news = invocation.getArgument(0);
            savedNews.add(news);
            return news;
        });

        NewsPublicDataSyncService.NewsSyncResult result = syncService.sync();

        assertThat(result.created()).isEqualTo(3);
        assertThat(savedNews)
                .extracting(News::getRegionId)
                .containsExactly(1L, 2L, 3L);
        then(regionRepository).should(never()).save(any(Region.class));
    }

    @Test
    void syncRejectsUnknownRegionInsteadOfCreatingExternalRegion() {
        NewsSource source = mock(NewsSource.class);
        NewsCategory category = mock(NewsCategory.class);
        NewsCandidate candidate = candidate("unknown-region", "미등록 지역 소식", "없는도 없는시");

        given(regionRepository.findAllByOrderByRegionLevel1AscRegionLevel2Asc())
                .willReturn(List.of());
        given(collector.getSourceType()).willReturn(NewsSourceType.PUBLIC_API);
        given(collector.sourceName()).willReturn("테스트 공공데이터");
        given(newsSourceRepository.findBySourceTypeAndName(
                NewsSourceType.PUBLIC_API,
                "테스트 공공데이터"
        )).willReturn(Optional.of(source));
        given(collector.collect(source)).willReturn(List.of(candidate));
        given(newsRepository.findAllByNewsSourceAndExternalItemIdIn(
                source,
                List.of("unknown-region")
        )).willReturn(List.of());
        given(newsCategoryRepository.findByNewsTypeAndName(
                NewsType.LOCAL,
                "SUPPORT_SERVICE"
        )).willReturn(Optional.of(category));

        assertThatThrownBy(syncService::sync)
                .isInstanceOf(ProjectException.class);
        then(regionRepository).should(never()).save(any(Region.class));
        then(newsRepository).should(never()).save(any(News.class));
    }

    private NewsCandidate candidate(String externalItemId, String title) {
        return candidate(externalItemId, title, null);
    }

    private NewsCandidate candidate(String externalItemId, String title, String regionName) {
        return new NewsCandidate(
                externalItemId,
                title,
                "요약",
                "내용",
                "기관",
                "https://example.com/" + externalItemId,
                null,
                regionName,
                null,
                null,
                null,
                LocalDateTime.of(2026, 7, 26, 10, 0),
                null,
                null,
                null,
                null,
                "SUPPORT_SERVICE",
                NewsType.LOCAL,
                RecruitmentStatus.OPEN
        );
    }

    private Region region(Long id, String fullName) {
        Region region = mock(Region.class);
        given(region.getId()).willReturn(id);
        given(region.getFullName()).willReturn(fullName);
        return region;
    }
}
