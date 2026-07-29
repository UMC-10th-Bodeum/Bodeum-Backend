package com.bodeum.domain.news.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import com.bodeum.domain.news.collector.NewsCandidate;
import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.entity.NewsCategory;
import com.bodeum.domain.news.entity.NewsCategoryCode;
import com.bodeum.domain.news.entity.NewsSource;
import com.bodeum.domain.news.entity.NewsSourceType;
import com.bodeum.domain.news.entity.NewsType;
import com.bodeum.domain.news.entity.RecruitmentStatus;
import com.bodeum.domain.news.infrastructure.openapi.PublicDataNewsCollector;
import com.bodeum.domain.news.repository.NewsCategoryRepository;
import com.bodeum.domain.news.repository.NewsRepository;
import com.bodeum.domain.news.repository.NewsSourceRepository;
import com.bodeum.domain.region.repository.RegionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
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
                "LOCAL_NEWS"
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
    void syncStoresTheApiBaseUrlProvidedByEachCollector() {
        given(regionRepository.findAllByOrderByRegionLevel1AscRegionLevel2Asc())
                .willReturn(List.of());
        given(collector.getSourceType()).willReturn(NewsSourceType.PUBLIC_API);
        given(collector.sourceName()).willReturn("경기도 장애인복지관 운영 프로그램");
        given(collector.sourceApiBaseUrl()).willReturn("https://openapi.gg.go.kr");
        given(collector.sourceListUrl()).willReturn("https://data.gg.go.kr/dataset");
        given(newsSourceRepository.findBySourceTypeAndName(
                NewsSourceType.PUBLIC_API,
                "경기도 장애인복지관 운영 프로그램"
        )).willReturn(Optional.empty());
        given(newsSourceRepository.save(any(NewsSource.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(collector.collect(any(NewsSource.class))).willReturn(List.of());

        syncService.sync();

        ArgumentCaptor<NewsSource> sourceCaptor = ArgumentCaptor.forClass(NewsSource.class);
        then(newsSourceRepository).should().save(sourceCaptor.capture());
        assertThat(sourceCaptor.getValue().getBaseUrl()).isEqualTo("https://openapi.gg.go.kr");
        assertThat(sourceCaptor.getValue().getListUrl()).isEqualTo("https://data.gg.go.kr/dataset");
    }
    private NewsCandidate candidate(String externalItemId, String title) {
        return new NewsCandidate(
                externalItemId,
                title,
                "요약",
                "내용",
                "기관",
                "https://example.com/" + externalItemId,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 7, 26, 10, 0),
                null,
                null,
                null,
                null,
                NewsCategoryCode.LOCAL_NEWS,
                NewsType.LOCAL,
                RecruitmentStatus.OPEN
        );
    }
}
