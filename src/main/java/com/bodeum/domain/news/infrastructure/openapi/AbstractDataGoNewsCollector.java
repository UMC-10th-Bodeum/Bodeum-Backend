package com.bodeum.domain.news.infrastructure.openapi;

import com.bodeum.domain.news.collector.NewsCandidate;
import com.bodeum.domain.news.entity.NewsSource;
import com.bodeum.global.infrastructure.openapi.DataGoOpenApiClient;
import com.bodeum.global.infrastructure.openapi.DataGoOpenApiPageResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractDataGoNewsCollector implements PublicDataNewsCollector {

    private static final String API_BASE_URL = "https://api.data.go.kr/openapi";
    private static final int PAGE_SIZE = 500;

    private final DataGoOpenApiClient dataGoOpenApiClient;

    protected AbstractDataGoNewsCollector(DataGoOpenApiClient dataGoOpenApiClient) {
        this.dataGoOpenApiClient = dataGoOpenApiClient;
    }

    @Override
    public final String sourceApiBaseUrl() {
        return API_BASE_URL;
    }

    @Override
    public final List<NewsCandidate> collect(NewsSource newsSource) {
        Map<String, NewsCandidate> candidatesByExternalId = new LinkedHashMap<>();
        int page = 1;
        int totalPages;

        do {
            DataGoOpenApiPageResponse response = dataGoOpenApiClient.fetchPage(
                    resourcePath(),
                    page,
                    PAGE_SIZE
            );
            for (Map<String, Object> row : response.rows()) {
                NewsCandidate candidate = map(row);
                if (candidate != null) {
                    candidatesByExternalId.put(candidate.externalItemId(), candidate);
                }
            }
            totalPages = Math.max(1, (int) Math.ceil((double) response.totalCount() / PAGE_SIZE));
            page++;
        } while (page <= totalPages);

        return List.copyOf(candidatesByExternalId.values());
    }

    protected abstract String resourcePath();

    protected abstract NewsCandidate map(Map<String, Object> row);
}
