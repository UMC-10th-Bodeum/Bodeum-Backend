package com.bodeum.domain.news.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bodeum.domain.news.collector.NewsCandidate;
import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.entity.NewsCategory;
import com.bodeum.domain.news.entity.NewsSource;
import com.bodeum.domain.news.entity.NewsSourceType;
import com.bodeum.domain.news.entity.NewsType;
import com.bodeum.domain.news.entity.RecruitmentStatus;
import com.bodeum.domain.news.repository.NewsCategoryRepository;
import com.bodeum.domain.news.repository.NewsRepository;
import com.bodeum.domain.news.repository.NewsScrapRepository;
import com.bodeum.domain.news.repository.NewsSourceRepository;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.region.repository.RegionRepository;
import com.bodeum.global.auth.AuthUserPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(properties = "bodeum.auth.jwt-secret=test-jwt-secret-32-bytes-minimum-value")
@AutoConfigureMockMvc
@Transactional
class NewsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private NewsCategoryRepository newsCategoryRepository;

    @Autowired
    private NewsSourceRepository newsSourceRepository;

    @Autowired
    private NewsScrapRepository newsScrapRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Test
    void getNewsFiltersByRegionCategoryAndStatusAndSortsLatestFirst() throws Exception {
        Region seoul = regionRepository.save(Region.create("서울특별시", "강남구"));
        Region suwon = regionRepository.save(Region.create("경기도", "수원시"));
        NewsCategory volunteer = newsCategoryRepository.save(
                NewsCategory.create(NewsType.ACTIVITY, "VOLUNTEER", 1)
        );
        NewsSource source = newsSourceRepository.save(source());
        News olderSeoulNews = newsRepository.save(news(
                volunteer,
                source,
                seoul.getId(),
                "seoul-old",
                "서울 이전 봉사",
                LocalDateTime.of(2026, 7, 1, 10, 0),
                RecruitmentStatus.OPEN
        ));
        News latestSeoulNews = newsRepository.save(news(
                volunteer,
                source,
                seoul.getId(),
                "seoul-latest",
                "서울 최신 봉사",
                LocalDateTime.of(2026, 7, 2, 10, 0),
                RecruitmentStatus.OPEN
        ));
        newsRepository.save(news(
                volunteer,
                source,
                suwon.getId(),
                "suwon",
                "수원 봉사",
                LocalDateTime.of(2026, 7, 3, 10, 0),
                RecruitmentStatus.OPEN
        ));

        mockMvc.perform(get("/api/news")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "latest")
                        .param("region", "서울")
                        .param("category", "VOLUNTEER")
                        .param("status", "RECRUITING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200_1"))
                .andExpect(jsonPath("$.result.totalElements").value(2))
                .andExpect(jsonPath("$.result.items[0].newsId").value(latestSeoulNews.getId()))
                .andExpect(jsonPath("$.result.items[0].status").value("RECRUITING"))
                .andExpect(jsonPath("$.result.items[0].region").value("서울특별시 강남구"))
                .andExpect(jsonPath("$.result.items[1].newsId").value(olderSeoulNews.getId()));
    }

    @Test
    void getNewsDetailReturnsContentAndIncreasesViewCount() throws Exception {
        Region region = regionRepository.save(Region.create("경기도", "수원시"));
        NewsCategory category = newsCategoryRepository.save(
                NewsCategory.create(NewsType.LOCAL, "SUPPORT_SERVICE", 1)
        );
        NewsSource source = newsSourceRepository.save(source());
        News news = newsRepository.saveAndFlush(news(
                category,
                source,
                region.getId(),
                "detail",
                "상세 소식",
                LocalDateTime.of(2026, 7, 2, 10, 0),
                RecruitmentStatus.OPEN
        ));

        mockMvc.perform(get("/api/news/{newsId}", news.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.newsId").value(news.getId()))
                .andExpect(jsonPath("$.result.title").value("상세 소식"))
                .andExpect(jsonPath("$.result.region").value("경기도 수원시"))
                .andExpect(jsonPath("$.result.viewCount").value(1))
                .andExpect(jsonPath("$.result.scrapped").value(false));

        assertThat(newsRepository.findById(news.getId()).orElseThrow().getViewCount()).isEqualTo(1L);
    }

    @Test
    void getNewsDetailReturnsCommonNotFoundResponse() throws Exception {
        mockMvc.perform(get("/api/news/{newsId}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON404_1"))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    void getNewsRejectsInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/news").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));
    }

    @Test
    void searchNewsMatchesTitleContentSourceNameAndRegionAndSortsLatestFirst() throws Exception {
        Region seoul = regionRepository.save(Region.create("서울특별시", "강남구"));
        Region suwon = regionRepository.save(Region.create("경기도", "수원시"));
        NewsCategory volunteer = newsCategoryRepository.save(
                NewsCategory.create(NewsType.ACTIVITY, "VOLUNTEER", 1)
        );
        NewsSource source = newsSourceRepository.save(source());
        News titleMatch = newsRepository.save(news(
                volunteer,
                source,
                seoul.getId(),
                "title-match",
                "맞춤 봉사 모집",
                "제목 검색 결과",
                "일반 내용",
                "테스트 기관",
                LocalDateTime.of(2026, 7, 1, 10, 0),
                RecruitmentStatus.OPEN
        ));
        News contentMatch = newsRepository.save(news(
                volunteer,
                source,
                seoul.getId(),
                "content-match",
                "언어치료 프로그램",
                "프로그램 안내",
                "장애아동 봉사 활동을 진행합니다.",
                "테스트 기관",
                LocalDateTime.of(2026, 7, 2, 10, 0),
                RecruitmentStatus.OPEN
        ));
        News sourceMatch = newsRepository.save(news(
                volunteer,
                source,
                seoul.getId(),
                "source-match",
                "가족 지원 프로그램",
                "프로그램 안내",
                "일반 내용",
                "행복봉사센터",
                LocalDateTime.of(2026, 7, 3, 10, 0),
                RecruitmentStatus.OPEN
        ));
        News regionMatch = newsRepository.save(news(
                volunteer,
                source,
                suwon.getId(),
                "region-match",
                "재활 프로그램",
                "프로그램 안내",
                "일반 내용",
                "테스트 기관",
                LocalDateTime.of(2026, 7, 4, 10, 0),
                RecruitmentStatus.OPEN
        ));

        mockMvc.perform(get("/api/news/search")
                        .param("keyword", "봉사")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(3))
                .andExpect(jsonPath("$.result.items[0].newsId").value(sourceMatch.getId()))
                .andExpect(jsonPath("$.result.items[1].newsId").value(contentMatch.getId()))
                .andExpect(jsonPath("$.result.items[2].newsId").value(titleMatch.getId()));

        mockMvc.perform(get("/api/news/search")
                        .param("keyword", "수원")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.items[0].newsId").value(regionMatch.getId()))
                .andExpect(jsonPath("$.result.items[0].region").value("경기도 수원시"));
    }

    @Test
    void searchNewsAppliesRegionCategoryAndStatusFilters() throws Exception {
        Region seoul = regionRepository.save(Region.create("서울특별시", "강남구"));
        Region suwon = regionRepository.save(Region.create("경기도", "수원시"));
        NewsCategory volunteer = newsCategoryRepository.save(
                NewsCategory.create(NewsType.ACTIVITY, "VOLUNTEER", 1)
        );
        NewsCategory support = newsCategoryRepository.save(
                NewsCategory.create(NewsType.LOCAL, "SUPPORT_SERVICE", 2)
        );
        NewsSource source = newsSourceRepository.save(source());
        News expected = newsRepository.save(news(
                volunteer,
                source,
                seoul.getId(),
                "expected",
                "서울 봉사 모집",
                LocalDateTime.of(2026, 7, 1, 10, 0),
                RecruitmentStatus.OPEN
        ));
        newsRepository.save(news(
                volunteer,
                source,
                suwon.getId(),
                "wrong-region",
                "수원 봉사 모집",
                LocalDateTime.of(2026, 7, 2, 10, 0),
                RecruitmentStatus.OPEN
        ));
        newsRepository.save(news(
                support,
                source,
                seoul.getId(),
                "wrong-category",
                "서울 봉사 지원",
                LocalDateTime.of(2026, 7, 3, 10, 0),
                RecruitmentStatus.OPEN
        ));
        newsRepository.save(news(
                volunteer,
                source,
                seoul.getId(),
                "wrong-status",
                "서울 봉사 마감",
                LocalDateTime.of(2026, 7, 4, 10, 0),
                RecruitmentStatus.CLOSED
        ));

        mockMvc.perform(get("/api/news/search")
                        .param("keyword", "봉사")
                        .param("region", "서울")
                        .param("category", "VOLUNTEER")
                        .param("status", "RECRUITING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.items[0].newsId").value(expected.getId()));
    }

    @Test
    void searchNewsRejectsBlankKeyword() throws Exception {
        mockMvc.perform(get("/api/news/search").param("keyword", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));
    }

    @Test
    void toggleNewsScrapAddsAndRemovesScrap() throws Exception {
        NewsCategory category = newsCategoryRepository.save(
                NewsCategory.create(NewsType.LOCAL, "SUPPORT_SERVICE", 1)
        );
        NewsSource source = newsSourceRepository.save(source());
        News news = newsRepository.saveAndFlush(news(
                category,
                source,
                null,
                "scrap-toggle",
                "스크랩 토글 소식",
                LocalDateTime.of(2026, 7, 2, 10, 0),
                RecruitmentStatus.OPEN
        ));

        mockMvc.perform(post("/api/news/{newsId}/scrap", news.getId())
                        .with(authenticatedUser(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200_1"))
                .andExpect(jsonPath("$.result.newsId").value(news.getId()))
                .andExpect(jsonPath("$.result.scrapped").value(true))
                .andExpect(jsonPath("$.result.scrapCount").value(1));

        assertThat(newsScrapRepository.existsByNewsIdAndUserId(news.getId(), 10L)).isTrue();
        assertThat(newsRepository.findById(news.getId()).orElseThrow().getScrapCount()).isOne();

        mockMvc.perform(post("/api/news/{newsId}/scrap", news.getId())
                        .with(authenticatedUser(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.newsId").value(news.getId()))
                .andExpect(jsonPath("$.result.scrapped").value(false))
                .andExpect(jsonPath("$.result.scrapCount").value(0));

        assertThat(newsScrapRepository.existsByNewsIdAndUserId(news.getId(), 10L)).isFalse();
        assertThat(newsRepository.findById(news.getId()).orElseThrow().getScrapCount()).isZero();
    }

    @Test
    void toggleNewsScrapRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/news/{newsId}/scrap", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void toggleNewsScrapReturnsCommonNotFoundResponse() throws Exception {
        mockMvc.perform(post("/api/news/{newsId}/scrap", 999999L)
                        .with(authenticatedUser(10L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON404_1"));
    }

    private RequestPostProcessor authenticatedUser(Long userId) {
        AuthUserPrincipal principal = new AuthUserPrincipal(userId, null, null, null);
        return authentication(new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));
    }

    private NewsSource source() {
        return NewsSource.create(
                NewsSourceType.PUBLIC_API,
                "테스트 출처",
                "https://example.test",
                "https://example.test/news"
        );
    }

    private News news(
            NewsCategory category,
            NewsSource source,
            Long regionId,
            String externalId,
            String title,
            LocalDateTime publishedAt,
            RecruitmentStatus status
    ) {
        return news(
                category,
                source,
                regionId,
                externalId,
                title,
                title + " 요약",
                title + " 상세 내용",
                "테스트 기관",
                publishedAt,
                status
        );
    }

    private News news(
            NewsCategory category,
            NewsSource source,
            Long regionId,
            String externalId,
            String title,
            String summary,
            String content,
            String sourceName,
            LocalDateTime publishedAt,
            RecruitmentStatus status
    ) {
        NewsCandidate candidate = new NewsCandidate(
                externalId,
                title,
                summary,
                content,
                sourceName,
                "https://example.test/" + externalId,
                null,
                null,
                "장애아동",
                "031-000-0000",
                null,
                publishedAt,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 12, 31),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 12, 31),
                category.getName(),
                category.getNewsType(),
                status
        );
        return News.create(category, source, regionId, candidate);
    }
}
