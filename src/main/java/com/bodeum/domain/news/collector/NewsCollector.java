package com.bodeum.domain.news.collector;

import com.bodeum.domain.news.entity.NewsSource;
import com.bodeum.domain.news.entity.NewsSourceType;

import java.util.List;

public interface NewsCollector {

    NewsSourceType getSourceType();

    List<NewsCandidate> collect(NewsSource newsSource);
}
