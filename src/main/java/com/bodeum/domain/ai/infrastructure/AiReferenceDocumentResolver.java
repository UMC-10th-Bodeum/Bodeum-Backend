package com.bodeum.domain.ai.infrastructure;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.model.AiReferenceDocument;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.repository.NewsRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AiReferenceDocumentResolver {

    private final InfoItemRepository infoItemRepository;
    private final NewsRepository newsRepository;
    private final ZoneId sourceTimeZone;

    public AiReferenceDocumentResolver(
            InfoItemRepository infoItemRepository,
            NewsRepository newsRepository,
            @Value("${bodeum.ai.indexing.source-time-zone:Asia/Seoul}") String sourceTimeZone
    ) {
        this.infoItemRepository = infoItemRepository;
        this.newsRepository = newsRepository;
        this.sourceTimeZone = ZoneId.of(sourceTimeZone);
    }

    @Transactional(readOnly = true)
    public List<AiReferenceDocument> resolve(List<AiReferenceDocument> retrievedDocuments) {
        Set<Long> infoIds = sourceIds(retrievedDocuments, AiResponseSourceType.INFO);
        Set<Long> newsIds = sourceIds(retrievedDocuments, AiResponseSourceType.NEWS);
        Map<Long, InfoItem> infoById = infoIds.isEmpty()
                ? Map.of()
                : infoItemRepository.findAllIndexableByIdIn(infoIds).stream()
                        .collect(Collectors.toMap(InfoItem::getId, Function.identity()));
        Map<Long, News> newsById = newsIds.isEmpty()
                ? Map.of()
                : newsRepository.findAllIndexableByIdIn(newsIds).stream()
                        .collect(Collectors.toMap(News::getId, Function.identity()));

        Map<String, AiReferenceDocument> resolved = new LinkedHashMap<>();
        for (AiReferenceDocument document : retrievedDocuments) {
            AiReferenceDocument current = switch (document.sourceType()) {
                case INFO -> resolveInfo(document, infoById.get(document.sourceId()));
                case NEWS -> resolveNews(document, newsById.get(document.sourceId()));
                case SITE -> document;
            };
            if (current != null) {
                resolved.putIfAbsent(sourceKey(current), current);
            }
        }
        return new ArrayList<>(resolved.values());
    }

    private Set<Long> sourceIds(
            List<AiReferenceDocument> documents,
            AiResponseSourceType sourceType
    ) {
        return documents.stream()
                .filter(document -> document.sourceType() == sourceType)
                .map(AiReferenceDocument::sourceId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private AiReferenceDocument resolveInfo(AiReferenceDocument document, InfoItem info) {
        if (info == null) {
            return null;
        }
        return new AiReferenceDocument(
                document.documentKey(),
                infoContent(info),
                AiResponseSourceType.INFO,
                info.getId(),
                info.getName(),
                info.getHomepageUrl(),
                sourceUpdatedAt(info.getUpdatedAt(), info.getCreatedAt(),
                        () -> info.getSyncedAt().atZone(sourceTimeZone).toInstant())
        );
    }

    private AiReferenceDocument resolveNews(AiReferenceDocument document, News news) {
        if (news == null) {
            return null;
        }
        return new AiReferenceDocument(
                document.documentKey(),
                newsContent(news),
                AiResponseSourceType.NEWS,
                news.getId(),
                news.getTitle(),
                news.getOriginalUrl(),
                sourceUpdatedAt(news.getUpdatedAt(), news.getCreatedAt(),
                        () -> news.getPublishedAt().atZone(sourceTimeZone).toInstant())
        );
    }

    private String infoContent(InfoItem info) {
        InfoCategory category = info.getInfoCategory();
        return lines(
                line("정보명", info.getName()),
                line("대분류", category.getMainCategoryKo()),
                line("대분류 코드", category.getMainCategory().name()),
                line("세부 분류", category.getSubCategoryKo()),
                line("세부 분류 코드", category.getSubCategory()),
                line("소개", info.getIntroduction()),
                line("주소", info.getAddress()),
                line("지역", "%s %s".formatted(info.getSido(), info.getSigungu())),
                line("전화번호", info.getPhone()),
                line("홈페이지", info.getHomepageUrl())
        );
    }

    private String newsContent(News news) {
        return lines(
                line("소식명", news.getTitle()),
                line("카테고리", news.getNewsCategory().getName()),
                line("소식 유형", news.getNewsType()),
                line("요약", news.getSummary()),
                line("본문", news.getContent()),
                line("제공 기관", newsSourceName(news)),
                line("게시일", news.getPublishedAt()),
                line("대상", news.getTargetAudience()),
                line("신청 기간", dateRange(news.getApplyStartDate(), news.getApplyEndDate())),
                line("프로그램 기간", dateRange(news.getProgramStartDate(), news.getProgramEndDate())),
                line("모집 상태", news.getRecruitmentStatus()),
                line("연락처", news.getContact()),
                line("담당자", news.getManager()),
                line("원문", news.getOriginalUrl())
        );
    }

    private String newsSourceName(News news) {
        if (news.getSourceName() != null && !news.getSourceName().isBlank()) {
            return news.getSourceName();
        }
        return news.getNewsSource() == null ? null : news.getNewsSource().getName();
    }

    private Instant sourceUpdatedAt(
            Instant updatedAt,
            Instant createdAt,
            Supplier<Instant> fallback
    ) {
        if (updatedAt != null) {
            return updatedAt;
        }
        return createdAt != null ? createdAt : fallback.get();
    }

    private String sourceKey(AiReferenceDocument document) {
        return document.sourceType() + ":" + document.sourceId();
    }

    private String line(String label, Object value) {
        return value == null || value.toString().isBlank() ? "" : label + ": " + value;
    }

    private String lines(String... values) {
        return String.join("\n", List.of(values).stream().filter(value -> !value.isBlank()).toList());
    }

    private String dateRange(Object start, Object end) {
        if (start == null && end == null) {
            return "";
        }
        return "%s ~ %s".formatted(start == null ? "미정" : start, end == null ? "미정" : end);
    }
}
