package com.bodeum.domain.news.infrastructure.openapi;

import com.bodeum.domain.news.collector.NewsCandidate;
import com.bodeum.domain.news.entity.NewsSource;
import com.bodeum.global.infrastructure.openapi.OdcloudClient;
import com.bodeum.global.infrastructure.openapi.OdcloudPageResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractOdcloudNewsCollector implements PublicDataNewsCollector {

    private static final int PAGE_SIZE = 100;

    private final OdcloudClient odcloudClient;

    protected AbstractOdcloudNewsCollector(OdcloudClient odcloudClient) {
        this.odcloudClient = odcloudClient;
    }

    @Override
    public final List<NewsCandidate> collect(NewsSource newsSource) {
        List<NewsCandidate> candidates = new ArrayList<>();
        int page = 1;
        int totalPages;

        do {
            OdcloudPageResponse response = odcloudClient.fetchPage(resourcePath(), page, PAGE_SIZE);
            response.data().stream()
                    .map(this::map)
                    .filter(Objects::nonNull)
                    .forEach(candidates::add);
            totalPages = Math.max(1, (int) Math.ceil((double) response.totalCount() / PAGE_SIZE));
            page++;
        } while (page <= totalPages);

        return List.copyOf(candidates);
    }

    protected abstract String resourcePath();

    protected abstract NewsCandidate map(Map<String, Object> row);
}
