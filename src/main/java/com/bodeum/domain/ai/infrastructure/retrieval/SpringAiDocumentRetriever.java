package com.bodeum.domain.ai.infrastructure.retrieval;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
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
    public List<AiReferenceDocument> retrieve(String question, AiUserProfile profile) {
        try {
            String searchQuery = buildSearchQuery(question, profile);
            List<Document> personalizedDocuments = search(searchQuery);
            List<Document> questionDocuments = search(question);

            Map<String, Document> documentsById = new LinkedHashMap<>();
            personalizedDocuments.forEach(document -> documentsById.put(document.getId(), document));
            questionDocuments.forEach(document -> documentsById.merge(
                    document.getId(), document, this::higherScore));

            documentsById.values().forEach(document -> log.debug(
                    "[AI] RAG candidate: id={}, score={}, threshold={}",
                    document.getId(), score(document), similarityThreshold));

            return documentsById.values().stream()
                    .filter(document -> score(document) >= similarityThreshold)
                    .sorted(Comparator.comparingDouble(this::score).reversed())
                    .limit(topK)
                    .map(this::mapDocument)
                    .toList();
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            throw new ProjectException(AiErrorCode.AI_RESPONSE_FAILED, e);
        }
    }

    private List<Document> search(String query) {
        return vectorStoreRetriever.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.0)
                .build());
    }

    private Document higherScore(Document left, Document right) {
        return score(left) >= score(right) ? left : right;
    }

    private double score(Document document) {
        return document.getScore() == null ? 0.0 : document.getScore();
    }

    private String buildSearchQuery(String question, AiUserProfile profile) {
        StringBuilder query = new StringBuilder(question);
        append(query, "활동 지역", profile.region());
        append(query, "집중 케어 영역", String.join(", ", profile.disabilityTypes()));
        append(query, "관심사", String.join(", ", profile.interests()));
        append(query, "자녀 관련 관심 키워드", profile.keywordText());
        return query.toString();
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
