package com.bodeum.domain.ai.infrastructure.indexing;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.model.indexing.AiIndexingResult;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.entity.NewsSource;
import com.bodeum.domain.news.repository.NewsRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class AiContentIndexingService {

    private final InfoItemRepository infoItemRepository;
    private final NewsRepository newsRepository;
    private final VectorStore vectorStore;
    private final AiIndexingCoordinator indexingCoordinator;
    private final TokenTextSplitter textSplitter;
    private final int writeBatchSize;
    private final ZoneId sourceTimeZone;

    public AiContentIndexingService(
            InfoItemRepository infoItemRepository,
            NewsRepository newsRepository,
            VectorStore vectorStore,
            AiIndexingCoordinator indexingCoordinator,
            @Value("${bodeum.ai.indexing.chunk-size:500}") int chunkSize,
            @Value("${bodeum.ai.indexing.write-batch-size:50}") int writeBatchSize,
            @Value("${bodeum.ai.indexing.source-time-zone:Asia/Seoul}") String sourceTimeZone
    ) {
        if (chunkSize <= 0 || writeBatchSize <= 0) {
            throw new IllegalArgumentException("AI 색인 크기 설정은 1 이상이어야 합니다.");
        }
        this.infoItemRepository = infoItemRepository;
        this.newsRepository = newsRepository;
        this.vectorStore = vectorStore;
        this.indexingCoordinator = indexingCoordinator;
        this.textSplitter = TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .withMinChunkSizeChars(200)
                .withMinChunkLengthToEmbed(10)
                .withKeepSeparator(true)
                .build();
        this.writeBatchSize = writeBatchSize;
        this.sourceTimeZone = ZoneId.of(sourceTimeZone);
    }

    public AiIndexingResult rebuildAll() {
        try {
            return indexingCoordinator.execute(this::doRebuildAll);
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            throw new ProjectException(AiErrorCode.AI_INDEXING_FAILED, e);
        }
    }

    public void synchronize(AiResponseSourceType sourceType, Long sourceId) {
        if (sourceId == null) {
            throw new IllegalArgumentException("Source ID to index must not be null");
        }
        try {
            indexingCoordinator.execute(() -> {
                synchronizeLatest(sourceType, sourceId);
                return null;
            });
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            throw new ProjectException(AiErrorCode.AI_INDEXING_FAILED, e);
        }
    }

    private AiIndexingResult doRebuildAll() {
        try {
            List<InfoItem> infoItems = infoItemRepository.findAllIndexable();
            List<News> newsItems = newsRepository.findAllIndexable();

            List<Document> documents = new ArrayList<>();
            infoItems.forEach(item -> documents.addAll(createInfoDocuments(item)));
            newsItems.forEach(item -> documents.addAll(createNewsDocuments(item)));

            vectorStore.delete("sourceType == 'INFO'");
            vectorStore.delete("sourceType == 'NEWS'");
            addInBatches(documents);

            return new AiIndexingResult(infoItems.size(), newsItems.size(), documents.size());
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            throw new ProjectException(AiErrorCode.AI_INDEXING_FAILED, e);
        }
    }

    private void synchronizeLatest(AiResponseSourceType sourceType, Long sourceId) {
        switch (sourceType) {
            case INFO -> infoItemRepository.findIndexableById(sourceId)
                    .ifPresentOrElse(
                            item -> replace(sourceType, sourceId, () -> createInfoDocuments(item)),
                            () -> delete(sourceType, sourceId)
                    );
            case NEWS -> newsRepository.findIndexableById(sourceId)
                    .ifPresentOrElse(
                            news -> replace(sourceType, sourceId, () -> createNewsDocuments(news)),
                            () -> delete(sourceType, sourceId)
                    );
            case SITE -> throw new IllegalArgumentException(
                    "SITE sources are not stored in the vector index");
        }
    }

    private void delete(AiResponseSourceType sourceType, Long sourceId) {
        vectorStore.delete(sourceFilter(sourceType, sourceId));
    }

    private void replace(
            AiResponseSourceType sourceType,
            Long sourceId,
            Supplier<List<Document>> documentSupplier
    ) {
        if (sourceId == null) {
            throw new IllegalArgumentException("저장되지 않은 원본 데이터는 색인할 수 없습니다.");
        }
        try {
            List<Document> documents = documentSupplier.get();
            // 원본의 chunk 수가 줄어든 경우, 남는 예전 chunk를 제거하기 위해
            // 동일 sourceType/sourceId 문서를 모두 삭제한 뒤 최신 chunk를 저장한다.
            // 삭제와 추가의 동시 실행은 AiIndexingCoordinator가 직렬화한다.
            vectorStore.delete(sourceFilter(sourceType, sourceId));
            addInBatches(documents);
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            throw new ProjectException(AiErrorCode.AI_INDEXING_FAILED, e);
        }
    }

    private List<Document> createInfoDocuments(InfoItem item) {
        InfoCategory category = item.getInfoCategory();
        Map<String, Object> metadata = baseMetadata(
                AiResponseSourceType.INFO,
                item.getId(),
                item.getName(),
                item.getHomepageUrl(),
                sourceUpdatedAt(item.getUpdatedAt(), item.getCreatedAt(), () ->
                        item.getSyncedAt().atZone(sourceTimeZone).toInstant())
        );
        metadata.put("infoCategoryId", category.getId());
        metadata.put("mainCategory", category.getMainCategory().name());
        metadata.put("mainCategoryKo", category.getMainCategoryKo());
        metadata.put("subCategory", category.getSubCategory());
        metadata.put("subCategoryKo", category.getSubCategoryKo());
        metadata.put("sido", item.getSido());
        metadata.put("sigungu", item.getSigungu());

        String content = lines(
                line("정보명", item.getName()),
                line("대분류", category.getMainCategoryKo()),
                line("대분류 코드", category.getMainCategory().name()),
                line("세부 분류", category.getSubCategoryKo()),
                line("세부 분류 코드", category.getSubCategory()),
                line("소개", item.getIntroduction()),
                line("주소", item.getAddress()),
                line("지역", "%s %s".formatted(item.getSido(), item.getSigungu())),
                line("전화번호", item.getPhone()),
                line("홈페이지", item.getHomepageUrl())
        );
        return split(AiResponseSourceType.INFO, item.getId(), content, metadata);
    }

    private List<Document> createNewsDocuments(News news) {
        NewsSource newsSource = news.getNewsSource();
        Map<String, Object> metadata = baseMetadata(
                AiResponseSourceType.NEWS,
                news.getId(),
                news.getTitle(),
                news.getOriginalUrl(),
                sourceUpdatedAt(news.getUpdatedAt(), news.getCreatedAt(), () ->
                        news.getPublishedAt().atZone(sourceTimeZone).toInstant())
        );
        metadata.put("newsCategoryId", news.getNewsCategory().getId());
        metadata.put("newsCategory", news.getNewsCategory().getName());
        metadata.put("newsType", news.getNewsType().name());
        if (newsSource != null) {
            metadata.put("newsSourceId", newsSource.getId());
            metadata.put("newsSourceType", newsSource.getSourceType().name());
            metadata.put("newsSourceName", newsSource.getName());
        }
        if (news.getRegionId() != null) {
            metadata.put("regionId", news.getRegionId());
        }

        String content = lines(
                line("소식명", news.getTitle()),
                line("카테고리", news.getNewsCategory().getName()),
                line("소식 유형", news.getNewsType().name()),
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
        return split(AiResponseSourceType.NEWS, news.getId(), content, metadata);
    }

    private String newsSourceName(News news) {
        if (news.getSourceName() != null && !news.getSourceName().isBlank()) {
            return news.getSourceName();
        }
        return news.getNewsSource() == null ? null : news.getNewsSource().getName();
    }

    private List<Document> split(
            AiResponseSourceType sourceType,
            Long sourceId,
            String content,
            Map<String, Object> metadata
    ) {
        List<Document> chunks = textSplitter.apply(List.of(new Document(content, metadata)));
        List<Document> indexed = new ArrayList<>(chunks.size());
        for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
            Map<String, Object> chunkMetadata = new LinkedHashMap<>(metadata);
            chunkMetadata.put("chunkIndex", chunkIndex);
            // 결정적인 ID를 사용해 같은 원본을 다시 색인해도 chunk가 중복 생성되지 않게 한다.
            indexed.add(new Document(
                    documentId(sourceType, sourceId, chunkIndex),
                    chunks.get(chunkIndex).getText(),
                    chunkMetadata
            ));
        }
        return indexed;
    }

    private Map<String, Object> baseMetadata(
            AiResponseSourceType sourceType,
            Long sourceId,
            String title,
            String sourceUrl,
            Instant updatedAt
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sourceType", sourceType.name());
        metadata.put("sourceId", sourceId);
        metadata.put("title", title);
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            metadata.put("sourceUrl", sourceUrl);
        }
        metadata.put("updatedAt", updatedAt.toString());
        return metadata;
    }

    private Instant sourceUpdatedAt(
            Instant updatedAt,
            Instant createdAt,
            Supplier<Instant> fallback
    ) {
        if (updatedAt != null) {
            return updatedAt;
        }
        if (createdAt != null) {
            return createdAt;
        }
        return fallback.get();
    }

    private void addInBatches(List<Document> documents) {
        for (int start = 0; start < documents.size(); start += writeBatchSize) {
            int end = Math.min(start + writeBatchSize, documents.size());
            vectorStore.add(documents.subList(start, end));
        }
    }

    private String sourceFilter(AiResponseSourceType sourceType, Long sourceId) {
        return "sourceType == '%s' && sourceId == %d".formatted(sourceType.name(), sourceId);
    }

    private String documentId(AiResponseSourceType sourceType, Long sourceId, int chunkIndex) {
        return "%s-%d-%d".formatted(sourceType.name(), sourceId, chunkIndex);
    }

    private String line(String label, Object value) {
        return value == null || value.toString().isBlank() ? "" : label + ": " + value;
    }

    private String lines(String... lines) {
        return String.join("\n", List.of(lines).stream().filter(line -> !line.isBlank()).toList());
    }

    private String dateRange(Object start, Object end) {
        if (start == null && end == null) {
            return "";
        }
        return "%s ~ %s".formatted(start == null ? "미정" : start, end == null ? "미정" : end);
    }
}
