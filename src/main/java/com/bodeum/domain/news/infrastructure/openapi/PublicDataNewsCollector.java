package com.bodeum.domain.news.infrastructure.openapi;

import com.bodeum.domain.news.collector.NewsCollector;
import com.bodeum.domain.news.entity.NewsSourceType;

public interface PublicDataNewsCollector extends NewsCollector {

    String sourceName();

    String sourceApiBaseUrl();

    String sourceListUrl();

    @Override
    default NewsSourceType getSourceType() {
        return NewsSourceType.PUBLIC_API;
    }
}
