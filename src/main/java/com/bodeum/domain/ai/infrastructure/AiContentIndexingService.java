package com.bodeum.domain.ai.infrastructure;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.model.AiIndexingResult;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.news.entity.News;
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
    private final TokenTextSplitter textSplitter;
    private final int writeBatchSize;
    private final ZoneId sourceTimeZone;

    public AiContentIndexingService(
            InfoItemRepository infoItemRepository,
            NewsRepository newsRepository,
            VectorStore vectorStore,
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
            List<InfoItem> infoItems = infoItemRepository.findAll();
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
            throw new ProjectException(AiErrorCode.AI_INDEXING_FAILED);
        }
    }

    public void replaceInfo(InfoItem item) {
        replace(AiResponseSourceType.INFO, item.getId(), () -> createInfoDocuments(item));
    }

    public void replaceNews(News news) {
        replace(AiResponseSourceType.NEWS, news.getId(), () -> createNewsDocuments(news));
    }

    public void delete(AiResponseSourceType sourceType, Long sourceId) {
        try {
            vectorStore.delete(sourceFilter(sourceType, sourceId));
        } catch (Exception e) {
            throw new ProjectException(AiErrorCode.AI_INDEXING_FAILED);
        }
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
            vectorStore.delete(sourceFilter(sourceType, sourceId));
            addInBatches(documentSupplier.get());
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            throw new ProjectException(AiErrorCode.AI_INDEXING_FAILED);
        }
    }

    private List<Document> createInfoDocuments(InfoItem item) {
        Map<String, Object> metadata = baseMetadata(
                AiResponseSourceType.INFO,
                item.getId(),
                item.getName(),
                item.getHomepageUrl(),
                sourceUpdatedAt(item.getUpdatedAt(), item.getCreatedAt(), () ->
                        item.getSyncedAt().atZone(sourceTimeZone).toInstant())
        );
        metadata.put("category", item.getCategory().name());
        metadata.put("sido", item.getSido());
        metadata.put("sigungu", item.getSigungu());

        String content = lines(
                line("정보명", item.getName()),
                line("카테고리", item.getCategory().name()),
                line("소개", item.getIntroduction()),
                line("주소", item.getAddress()),
                line("지역", "%s %s".formatted(item.getSido(), item.getSigungu())),
                line("전화번호", item.getPhone()),
                line("홈페이지", item.getHomepageUrl())
        );
        return split(AiResponseSourceType.INFO, item.getId(), content, metadata);
    }

    private List<Document> createNewsDocuments(News news) {
        Map<String, Object> metadata = baseMetadata(
                AiResponseSourceType.NEWS,
                news.getId(),
                news.getTitle(),
                news.getOriginalUrl(),
                sourceUpdatedAt(news.getUpdatedAt(), news.getCreatedAt(), () ->
                        news.getPublishedAt().atZone(sourceTimeZone).toInstant())
        );
        metadata.put("category", news.getNewsCategory().getName());
        metadata.put("newsType", news.getNewsType().name());
        if (news.getRegionId() != null) {
            metadata.put("regionId", news.getRegionId());
        }

        String content = lines(
                line("소식명", news.getTitle()),
                line("카테고리", news.getNewsCategory().getName()),
                line("소식 유형", news.getNewsType().name()),
                line("요약", news.getSummary()),
                line("본문", news.getContent()),
                line("제공 기관", news.getSourceName()),
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
