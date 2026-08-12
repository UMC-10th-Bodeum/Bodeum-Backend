package com.bodeum.domain.news.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.news.collector.NewsCandidate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class NewsTest {

    @Test
    void keepsOriginalUrlEmptyWhenNoVerifiedLinkExists() {
        News news = News.create(null, null, null, candidate(
                "확인되지 않은 기관",
                "확인되지 않은 프로그램",
                "https://www.data.go.kr/data/12345678/fileData.do"
        ));

        assertThat(news.getOriginalUrl()).isNull();
    }

    @Test
    void usesVerifiedInstitutionUrlInsteadOfPublicDataUrl() {
        News news = News.create(null, null, null, candidate(
                "구리시장애인종합복지관",
                "보행로봇재활",
                "https://www.data.go.kr/data/15108078/fileData.do"
        ));

        assertThat(news.getOriginalUrl()).isEqualTo("https://guriwel.or.kr/");
    }

    private NewsCandidate candidate(String sourceName, String title, String originalUrl) {
        return new NewsCandidate(
                "external-id",
                title,
                "요약",
                "내용",
                sourceName,
                originalUrl,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 8, 12, 0, 0),
                null,
                null,
                null,
                null,
                NewsCategoryCode.LOCAL_NEWS,
                NewsType.LOCAL,
                null
        );
    }
}
