package com.bodeum.domain.ai.infrastructure.indexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.enums.MainCategory;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.entity.NewsCategory;
import com.bodeum.domain.news.entity.NewsSource;
import com.bodeum.domain.news.entity.NewsSourceType;
import com.bodeum.domain.news.entity.NewsType;
import com.bodeum.domain.news.repository.NewsRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

class AiContentIndexingServiceTest {

    private final InfoItemRepository infoItemRepository = mock(InfoItemRepository.class);
    private final NewsRepository newsRepository = mock(NewsRepository.class);
    private final VectorStore vectorStore = mock(VectorStore.class);
    private final AiIndexingCoordinator indexingCoordinator = mock(AiIndexingCoordinator.class);

    @Test
    void rebuildsInfoAndNewsWithDeterministicDocumentIdsAndMetadata() {
        InfoItem info = infoItem();
        News news = news();
        when(infoItemRepository.findAllIndexable()).thenReturn(List.of(info));
        when(newsRepository.findAllIndexable()).thenReturn(List.of(news));
        List<Document> stored = new ArrayList<>();
        doAnswer(invocation -> {
            stored.addAll(invocation.getArgument(0));
            return null;
        }).when(vectorStore).add(anyList());
        AiContentIndexingService service = coordinatedService();

        var result = service.rebuildAll();

        assertThat(result.infoSourceCount()).isEqualTo(1);
        assertThat(result.newsSourceCount()).isEqualTo(1);
        assertThat(stored).extracting(Document::getId)
                .containsExactly("INFO-1-0", "NEWS-2-0");
        assertThat(stored.getFirst().getMetadata())
                .containsEntry("sourceType", "INFO")
                .containsEntry("sourceId", 1L)
                .containsEntry("infoCategoryId", 9L)
                .containsEntry("mainCategory", "INSTITUTION")
                .containsEntry("subCategory", "FAMILY_SUPPORT")
                .containsEntry("chunkIndex", 0)
                .containsEntry("sido", "경기도")
                .containsEntry("sigungu", "수원시");
        assertThat(stored.get(1).getMetadata())
                .containsEntry("sourceType", "NEWS")
                .containsEntry("sourceId", 2L)
                .containsEntry("newsCategoryId", 3L)
                .containsEntry("newsSourceId", 4L)
                .containsEntry("newsSourceType", "PUBLIC_API")
                .containsEntry("newsSourceName", "수원시청")
                .containsEntry("chunkIndex", 0);
        assertThat(stored.get(1).getText()).contains("제공 기관: 수원시 복지포털");
        verify(vectorStore).delete("sourceType == 'INFO'");
        verify(vectorStore).delete("sourceType == 'NEWS'");
    }

    @Test
    void reloadsLatestInfoAfterAcquiringIndexingLock() {
        InfoItem latestInfo = infoItem();
        when(infoItemRepository.findIndexableById(1L))
                .thenReturn(Optional.of(latestInfo));
        List<Document> stored = new ArrayList<>();
        doAnswer(invocation -> {
            stored.addAll(invocation.getArgument(0));
            return null;
        }).when(vectorStore).add(anyList());

        AiContentIndexingService service = coordinatedService();

        service.synchronize(AiResponseSourceType.INFO, 1L);

        verify(indexingCoordinator).execute(any());
        verify(infoItemRepository).findIndexableById(1L);
        verify(vectorStore).delete("sourceType == 'INFO' && sourceId == 1");
        assertThat(stored).extracting(Document::getId).containsExactly("INFO-1-0");
    }

    private AiContentIndexingService coordinatedService() {
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get())
                .when(indexingCoordinator).execute(any());
        return new AiContentIndexingService(
                infoItemRepository, newsRepository, vectorStore, indexingCoordinator,
                500, 50, "Asia/Seoul");
    }

    private InfoItem infoItem() {
        InfoCategory category = mock(InfoCategory.class);
        when(category.getId()).thenReturn(9L);
        when(category.getMainCategory()).thenReturn(MainCategory.INSTITUTION);
        when(category.getMainCategoryKo()).thenReturn("기관");
        when(category.getSubCategory()).thenReturn("FAMILY_SUPPORT");
        when(category.getSubCategoryKo()).thenReturn("장애인가족지원센터");
        InfoItem info = mock(InfoItem.class);
        when(info.getId()).thenReturn(1L);
        when(info.getName()).thenReturn("수원시 장애인가족지원센터");
        when(info.getInfoCategory()).thenReturn(category);
        when(info.getIntroduction()).thenReturn("장애인 가족 상담과 프로그램을 제공합니다.");
        when(info.getAddress()).thenReturn("경기도 수원시");
        when(info.getSido()).thenReturn("경기도");
        when(info.getSigungu()).thenReturn("수원시");
        when(info.getPhone()).thenReturn("031-000-0000");
        when(info.getHomepageUrl()).thenReturn("https://example.com/info/1");
        when(info.getUpdatedAt()).thenReturn(Instant.parse("2026-07-20T00:00:00Z"));
        return info;
    }

    private News news() {
        NewsCategory category = mock(NewsCategory.class);
        when(category.getId()).thenReturn(3L);
        when(category.getName()).thenReturn("복지 소식");
        NewsSource source = mock(NewsSource.class);
        when(source.getId()).thenReturn(4L);
        when(source.getSourceType()).thenReturn(NewsSourceType.PUBLIC_API);
        when(source.getName()).thenReturn("수원시청");
        News news = mock(News.class);
        when(news.getId()).thenReturn(2L);
        when(news.getTitle()).thenReturn("발달재활서비스 신청 안내");
        when(news.getNewsCategory()).thenReturn(category);
        when(news.getNewsSource()).thenReturn(source);
        when(news.getSourceName()).thenReturn("수원시 복지포털");
        when(news.getNewsType()).thenReturn(NewsType.LOCAL);
        when(news.getSummary()).thenReturn("신청 대상과 방법을 안내합니다.");
        when(news.getContent()).thenReturn("주민센터에서 신청할 수 있습니다.");
        when(news.getPublishedAt()).thenReturn(LocalDateTime.of(2026, 7, 20, 9, 0));
        when(news.getOriginalUrl()).thenReturn("https://example.com/news/2");
        when(news.getUpdatedAt()).thenReturn(Instant.parse("2026-07-20T00:00:00Z"));
        return news;
    }
}
