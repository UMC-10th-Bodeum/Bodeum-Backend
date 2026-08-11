package com.bodeum.domain.ai.infrastructure.retrieval;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.enums.AiSearchScope;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.service.port.AiDocumentRetriever;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStoreRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Profile("!test")
public class SpringAiDocumentRetriever implements AiDocumentRetriever {

    private static final Logger log = LoggerFactory.getLogger(SpringAiDocumentRetriever.class);
    private final VectorStoreRetriever vectorStoreRetriever;
    private final int topK;
    private final double similarityThreshold;

    public SpringAiDocumentRetriever(
            VectorStoreRetriever vectorStoreRetriever,
            @Value("${bodeum.ai.rag.top-k:5}") int topK,
            @Value("${bodeum.ai.rag.similarity-threshold:0.4}") double similarityThreshold
    ) {
        this.vectorStoreRetriever = vectorStoreRetriever;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    @Override
    public List<AiReferenceDocument> retrieve(
            String question,
            AiUserProfile profile,
            AiSearchScope searchScope
    ) {
        try {
            AiSearchScope resolvedScope = searchScope == null
                    ? AiSearchScope.GENERAL
                    : searchScope;
            if (resolvedScope == AiSearchScope.LOCAL_RESOURCE) {
                return retrieveLocalInstitution(question, profile);
            }
            return retrieveAtScope(
                    question,
                    profile,
                    null,
                    false
            );
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            throw new ProjectException(AiErrorCode.AI_RESPONSE_FAILED, e);
        }
    }

    private List<AiReferenceDocument> retrieveLocalInstitution(
            String question,
            AiUserProfile profile
    ) {
        if (hasText(profile.regionLevel1()) && hasText(profile.regionLevel2())) {
            String cityFilter = equalsFilter("sido", profile.regionLevel1())
                    + " && " + equalsFilter("sigungu", profile.regionLevel2());
            List<AiReferenceDocument> cityDocuments = retrieveAtScope(
                    question, profile, cityFilter, false);
            if (!cityDocuments.isEmpty()) {
                log.info("[AI] 지역 기관 검색 완료: scope=SIGUNGU, count={}",
                        cityDocuments.size());
                return cityDocuments;
            }
        }

        if (hasText(profile.regionLevel1())) {
            List<AiReferenceDocument> provinceDocuments = retrieveAtScope(
                    question,
                    profile,
                    equalsFilter("sido", profile.regionLevel1()),
                    false
            );
            if (!provinceDocuments.isEmpty()) {
                log.info("[AI] 지역 기관 검색 범위 확대: scope=SIDO, count={}",
                        provinceDocuments.size());
                return provinceDocuments;
            }
        }

        List<AiReferenceDocument> allDocuments = retrieveAtScope(
                question, profile, null, false);
        log.info("[AI] 지역 기관 검색 범위 확대: scope=ALL, count={}", allDocuments.size());
        return allDocuments;
    }

    private List<AiReferenceDocument> retrieveAtScope(
            String question,
            AiUserProfile profile,
            String filterExpression,
            boolean includeRegion
    ) {
        String searchQuery = buildSearchQuery(question, profile, includeRegion);
        List<Document> personalizedDocuments = search(searchQuery, filterExpression);
        List<Document> questionDocuments = search(question, filterExpression);

        Map<String, Document> documentsById = new LinkedHashMap<>();
        addByScore(documentsById, questionDocuments);
        addByScore(documentsById, personalizedDocuments);

        documentsById.values().forEach(document -> log.debug(
                "[AI] RAG candidate: id={}, score={}, threshold={}",
                document.getId(), score(document), similarityThreshold));

        return documentsById.values().stream()
                .limit(topK)
                .map(this::mapDocument)
                .toList();
    }

    private void addByScore(
            Map<String, Document> documentsById,
            List<Document> documents
    ) {
        documents.stream()
                .filter(document -> score(document) >= similarityThreshold)
                .sorted(Comparator.comparingDouble(this::score).reversed())
                .forEach(document -> documentsById.putIfAbsent(
                        document.getId(), document));
    }

    private List<Document> search(String query, String filterExpression) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.0);
        if (hasText(filterExpression)) {
            builder.filterExpression(filterExpression);
        }
        return vectorStoreRetriever.similaritySearch(builder.build());
    }

    private double score(Document document) {
        return document.getScore() == null ? 0.0 : document.getScore();
    }

    private String buildSearchQuery(
            String question,
            AiUserProfile profile,
            boolean includeRegion
    ) {
        StringBuilder query = new StringBuilder(question);
        if (includeRegion) {
            append(query, "활동 지역", profile.region());
        }
        append(query, "집중 케어 영역", String.join(", ", profile.disabilityTypes()));
        append(query, "관심사", String.join(", ", profile.interests()));
        append(query, "자녀 관련 관심 키워드", profile.keywordText());
        return query.toString();
    }

    private String equalsFilter(String field, String value) {
        return field + " == '" + value.replace("'", "\\'") + "'";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void append(StringBuilder query, String label, String value) {
        if (value != null && !value.isBlank()) {
            query.append('\n').append(label).append(": ").append(value);
        }
    }

    private AiReferenceDocument mapDocument(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        try {
            return new AiReferenceDocument(
                    document.getId(),
                    document.getText(),
                    AiResponseSourceType.valueOf(required(metadata, "sourceType")),
                    Long.valueOf(required(metadata, "sourceId")),
                    required(metadata, "title"),
                    nullable(metadata, "sourceUrl"),
                    Instant.parse(required(metadata, "updatedAt"))
            );
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new ProjectException(AiErrorCode.AI_INVALID_SOURCE_METADATA);
        }
    }

    private String required(Map<String, Object> metadata, String key) {
        String value = nullable(metadata, key);
        if (value == null || value.isBlank()) {
            throw new ProjectException(AiErrorCode.AI_INVALID_SOURCE_METADATA);
        }
        return value;
    }

    private String nullable(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value == null ? null : value.toString();
    }
}
