package com.bodeum.domain.ai.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.model.AiReferenceDocument;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.enums.MainCategory;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.news.repository.NewsRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiReferenceDocumentResolverTest {

    private final InfoItemRepository infoItemRepository = mock(InfoItemRepository.class);
    private final NewsRepository newsRepository = mock(NewsRepository.class);
    private final AiReferenceDocumentResolver resolver = new AiReferenceDocumentResolver(
            infoItemRepository, newsRepository, "Asia/Seoul");

    @Test
    void replacesVectorMetadataAndContentWithLatestMysqlInfoAndRemovesDuplicateChunks() {
        InfoItem info = mock(InfoItem.class);
        InfoCategory category = mock(InfoCategory.class);
        when(category.getMainCategory()).thenReturn(MainCategory.INSTITUTION);
        when(category.getMainCategoryKo()).thenReturn("기관");
        when(category.getSubCategory()).thenReturn("FAMILY_SUPPORT");
        when(category.getSubCategoryKo()).thenReturn("장애인가족지원센터");
        when(info.getId()).thenReturn(12L);
        when(info.getName()).thenReturn("최신 기관명");
        when(info.getInfoCategory()).thenReturn(category);
        when(info.getIntroduction()).thenReturn("최신 소개");
        when(info.getAddress()).thenReturn("경기도 수원시 팔달구");
        when(info.getSido()).thenReturn("경기도");
        when(info.getSigungu()).thenReturn("수원시");
        when(info.getPhone()).thenReturn("031-111-2222");
        when(info.getHomepageUrl()).thenReturn("https://new.example.com");
        when(info.getUpdatedAt()).thenReturn(Instant.parse("2026-07-20T01:00:00Z"));
        when(infoItemRepository.findAllIndexableByIdIn(any())).thenReturn(List.of(info));
        AiReferenceDocument firstChunk = staleInfo("INFO-12-0");
        AiReferenceDocument secondChunk = staleInfo("INFO-12-1");

        List<AiReferenceDocument> resolved = resolver.resolve(List.of(firstChunk, secondChunk));

        assertThat(resolved).hasSize(1);
        AiReferenceDocument document = resolved.getFirst();
        assertThat(document.title()).isEqualTo("최신 기관명");
        assertThat(document.url()).isEqualTo("https://new.example.com");
        assertThat(document.updatedAt()).isEqualTo(Instant.parse("2026-07-20T01:00:00Z"));
        assertThat(document.content())
                .contains("소개: 최신 소개")
                .contains("전화번호: 031-111-2222")
                .doesNotContain("이전 내용");
    }

    @Test
    void excludesDocumentWhenMysqlSourceNoLongerExists() {
        when(infoItemRepository.findAllIndexableByIdIn(any())).thenReturn(List.of());

        assertThat(resolver.resolve(List.of(staleInfo("INFO-12-0")))).isEmpty();
    }

    private AiReferenceDocument staleInfo(String documentKey) {
        return new AiReferenceDocument(
                documentKey,
                "소개: 이전 내용",
                AiResponseSourceType.INFO,
                12L,
                "이전 기관명",
                "https://old.example.com",
                Instant.parse("2026-07-01T00:00:00Z")
        );
    }
}
