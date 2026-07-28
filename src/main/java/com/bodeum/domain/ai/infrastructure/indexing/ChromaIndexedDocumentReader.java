package com.bodeum.domain.ai.infrastructure.indexing;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class ChromaIndexedDocumentReader implements AiIndexedDocumentReader {

    private static final int PAGE_SIZE = 500;

    private final ChromaApi chromaApi;
    private final ChromaVectorStoreProperties properties;

    public ChromaIndexedDocumentReader(
            ChromaApi chromaApi,
            ChromaVectorStoreProperties properties
    ) {
        this.chromaApi = chromaApi;
        this.properties = properties;
    }

    @Override
    public Set<String> findIds(AiResponseSourceType sourceType, Long sourceId) {
        ChromaApi.Collection collection = chromaApi.getCollection(
                properties.getTenantName(),
                properties.getDatabaseName(),
                properties.getCollectionName()
        );
        if (collection == null) {
            return Set.of();
        }

        Map<String, Object> where = where(sourceType, sourceId);
        Set<String> ids = new LinkedHashSet<>();
        for (int offset = 0; ; offset += PAGE_SIZE) {
            ChromaApi.GetEmbeddingResponse response = chromaApi.getEmbeddings(
                    properties.getTenantName(),
                    properties.getDatabaseName(),
                    collection.id(),
                    new ChromaApi.GetEmbeddingsRequest(
                            null, where, PAGE_SIZE, offset,
                            List.of(ChromaApi.QueryRequest.Include.METADATAS))
            );
            List<String> pageIds = response == null || response.ids() == null
                    ? List.of()
                    : response.ids();
            ids.addAll(pageIds);
            if (pageIds.size() < PAGE_SIZE) {
                return ids;
            }
        }
    }

    private Map<String, Object> where(AiResponseSourceType sourceType, Long sourceId) {
        if (sourceId == null) {
            return Map.of("sourceType", sourceType.name());
        }
        List<Map<String, Object>> conditions = new ArrayList<>();
        conditions.add(Map.of("sourceType", sourceType.name()));
        conditions.add(Map.of("sourceId", sourceId));
        Map<String, Object> where = new LinkedHashMap<>();
        where.put("$and", conditions);
        return where;
    }
}
