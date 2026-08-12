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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final Pattern REQUESTED_RESULT_COUNT_PATTERN =
            Pattern.compile("(?<!\\d)(\\d+)\\s*(?:개|곳)");
    private final VectorStoreRetriever vectorStoreRetriever;
    private final int topK;
    private final int maxResultCount;
    private final double similarityThreshold;

    public SpringAiDocumentRetriever(
            VectorStoreRetriever vectorStoreRetriever,
            @Value("${bodeum.ai.rag.top-k:5}") int topK,
            @Value("${bodeum.ai.result.max-count:10}") int maxResultCount,
            @Value("${bodeum.ai.rag.similarity-threshold:0.4}") double similarityThreshold
    ) {
        this.vectorStoreRetriever = vectorStoreRetriever;
        this.topK = topK;
        this.maxResultCount = Math.max(topK, maxResultCount);
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
                    categoryFilter(profile),
                    resolvedScope == AiSearchScope.GENERAL
                            && hasText(profile.region())
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
            String cityFilter = combineFilters(
                    equalsFilter("sido", profile.regionLevel1())
                            + " && " + equalsFilter("sigungu", profile.regionLevel2()),
                    categoryFilter(profile)
            );
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
                    combineFilters(
                            equalsFilter("sido", profile.regionLevel1()),
                            categoryFilter(profile)
                    ),
                    false
            );
            if (!provinceDocuments.isEmpty()) {
                log.info("[AI] 지역 기관 검색 범위 확대: scope=SIDO, count={}",
                        provinceDocuments.size());
                return provinceDocuments;
            }
        }

        List<AiReferenceDocument> allDocuments = retrieveAtScope(
                question, profile, categoryFilter(profile), false);
        log.info("[AI] 지역 기관 검색 범위 확대: scope=ALL, count={}", allDocuments.size());
        return allDocuments;
    }

    private List<AiReferenceDocument> retrieveAtScope(
            String question,
            AiUserProfile profile,
            String filterExpression,
            boolean includeRegion
    ) {
        int resultCount = resolveResultCount(question);
        String searchQuery = buildSearchQuery(question, profile, includeRegion);
        List<Document> personalizedDocuments = search(
                searchQuery, filterExpression, resultCount);
        List<Document> questionDocuments = search(
                question, filterExpression, resultCount);

        Map<String, Document> documentsById = new LinkedHashMap<>();
        if (includeRegion) {
            addRegionMatches(documentsById, personalizedDocuments, profile);
            addRegionMatches(documentsById, questionDocuments, profile);
        }
        addByScore(documentsById, questionDocuments);
        addByScore(documentsById, personalizedDocuments);

        documentsById.values().forEach(document -> log.debug(
                "[AI] RAG candidate: id={}, score={}, threshold={}",
                document.getId(), score(document), similarityThreshold));

        return documentsById.values().stream()
                .limit(resultCount)
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

    private List<Document> search(
            String query,
            String filterExpression,
            int resultCount
    ) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(resultCount)
                .similarityThreshold(0.0);
        if (hasText(filterExpression)) {
            builder.filterExpression(filterExpression);
        }
        return vectorStoreRetriever.similaritySearch(builder.build());
    }

    private void addRegionMatches(
            Map<String, Document> documentsById,
            List<Document> documents,
            AiUserProfile profile
    ) {
        documents.stream()
                .filter(document -> score(document) >= similarityThreshold)
                .filter(document -> matchesProfileRegion(document, profile))
                .sorted(Comparator.comparingDouble(this::score).reversed())
                .forEach(document -> documentsById.putIfAbsent(
                        document.getId(), document));
    }

    private boolean matchesProfileRegion(Document document, AiUserProfile profile) {
        Map<String, Object> metadata = document.getMetadata();
        String sido = nullable(metadata, "sido");
        String sigungu = nullable(metadata, "sigungu");
        if (hasText(profile.regionLevel1()) && hasText(profile.regionLevel2())) {
            return profile.regionLevel1().equals(sido)
                    && profile.regionLevel2().equals(sigungu);
        }
        return hasText(profile.regionLevel1())
                && profile.regionLevel1().equals(sido);
    }

    private int resolveResultCount(String question) {
        if (question == null || question.isBlank()) {
            return topK;
        }
        Matcher matcher = REQUESTED_RESULT_COUNT_PATTERN.matcher(question);
        int requestedCount = topK;
        while (matcher.find()) {
            try {
                requestedCount = Math.max(
                        requestedCount,
                        Integer.parseInt(matcher.group(1))
                );
            } catch (NumberFormatException ignored) {
                requestedCount = maxResultCount;
            }
        }
        return Math.min(requestedCount, maxResultCount);
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

    private String categoryFilter(AiUserProfile profile) {
        return profile.infoSubCategory() == null
                ? null
                : equalsFilter("subCategory", profile.infoSubCategory().name());
    }

    private String combineFilters(String first, String second) {
        if (!hasText(first)) {
            return second;
        }
        if (!hasText(second)) {
            return first;
        }
        return first + " && " + second;
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
