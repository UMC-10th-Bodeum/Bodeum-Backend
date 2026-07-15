package com.bodeum.domain.news.collector.jsoup;

import com.bodeum.domain.news.collector.NewsCandidate;
import com.bodeum.domain.news.collector.NewsCollector;
import com.bodeum.domain.news.entity.NewsSourceType;
import com.bodeum.domain.news.entity.NewsSource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class JsoupNewsCollector implements NewsCollector {

    private static final int TIMEOUT_MILLIS = 5000;
    private static final String USER_AGENT = "Mozilla/5.0";

    @Override
    public NewsSourceType getSourceType() {
        return NewsSourceType.STATIC_CRAWLING;
    }

    @Override
    public List<NewsCandidate> collect(NewsSource newsSource) {
        String listUrl = newsSource.getListUrl();

        if (listUrl == null || listUrl.isBlank()) {
            log.warn("정적 뉴스 크롤링 URL이 비어 있습니다. newsSourceId={}", newsSource.getId());
            return List.of();
        }

        try {
            Document document = Jsoup.connect(listUrl)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MILLIS)
                    .get();

            return parse(document);
        } catch (IOException e) {
            log.warn("정적 뉴스 크롤링에 실패했습니다. newsSourceId={}, listUrl={}", newsSource.getId(), listUrl, e);
            return List.of();
        }
    }

    private List<NewsCandidate> parse(Document document) {
        // TODO: 사이트별 selector 전략 적용 예정
        // 예: document.select(".notice-list li")
        return List.of();
    }
}
