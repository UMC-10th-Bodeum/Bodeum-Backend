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
import com.bodeum.domain.news.entity.NewsCategoryCode;
import com.bodeum.domain.news.entity.NewsSource;
import com.bodeum.domain.news.entity.NewsSourceType;
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
    void getNewsFiltersByRegionIdCategoryAndStatusAndSortsByViewCountByDefault() throws Exception {
        Region seoul = regionRepository.save(Region.create("서울특별시", "강남구"));
        Region suwon = regionRepository.save(Region.create("경기도", "수원시"));
        NewsCategory volunteer = newsCategoryRepository.save(
                NewsCategory.create(NewsCategoryCode.RECRUITMENT_PARTICIPATION)
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
        olderSeoulNews.increaseViewCount();
        olderSeoulNews.increaseViewCount();
        latestSeoulNews.increaseViewCount();
        newsRepository.save(news(
                volunteer,
                source,
                suwon.getId(),
                "suwon",
                "수원 봉사",
                LocalDateTime.of(2026, 7, 3, 10, 0),
                RecruitmentStatus.OPEN
        ));

        mockMvc.perform(get("/api/v1/news")
                        .param("page", "0")
                        .param("size", "10")
                        .param("regionId", seoul.getId().toString())
                        .param("category", "RECRUITMENT_PARTICIPATION")
                        .param("status", "RECRUITING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200_1"))
                .andExpect(jsonPath("$.result.totalElements").value(2))
                .andExpect(jsonPath("$.result.items[0].newsId").value(olderSeoulNews.getId()))
                .andExpect(jsonPath("$.result.items[0].status").value("RECRUITING"))
                .andExpect(jsonPath("$.result.items[0].categoryCode")
                        .value("RECRUITMENT_PARTICIPATION"))
                .andExpect(jsonPath("$.result.items[0].categoryLabel").value("모집 · 참여"))
                .andExpect(jsonPath("$.result.items[0].region").value("서울특별시 강남구"))
                .andExpect(jsonPath("$.result.items[0].publishedAt").value("2026-07-01"))
                .andExpect(jsonPath("$.result.items[1].newsId").value(latestSeoulNews.getId()));
    }

    @Test
    void getNewsSortsByScrapCount() throws Exception {
        Region region = regionRepository.save(Region.create("서울특별시", "강남구"));
        NewsCategory category = newsCategoryRepository.save(
                NewsCategory.create(NewsCategoryCode.RECRUITMENT_PARTICIPATION)
        );
        NewsSource source = newsSourceRepository.save(source());
        News lessScrapped = newsRepository.save(news(
                category,
                source,
                region.getId(),
                "less-scrapped",
                "저장 수가 적은 소식",
                LocalDateTime.of(2026, 7, 2, 10, 0),
                RecruitmentStatus.OPEN
        ));
        News moreScrapped = newsRepository.save(news(
                category,
                source,
                region.getId(),
                "more-scrapped",
                "저장 수가 많은 소식",
                LocalDateTime.of(2026, 7, 1, 10, 0),
                RecruitmentStatus.OPEN
        ));
        lessScrapped.increaseScrapCount();
        moreScrapped.increaseScrapCount();
        moreScrapped.increaseScrapCount();

        mockMvc.perform(get("/api/v1/news")
                        .param("sort", "SCRAP")
                        .param("regionId", region.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].newsId").value(moreScrapped.getId()))
                .andExpect(jsonPath("$.result.items[1].newsId").value(lessScrapped.getId()));
    }

    @Test
    void getNewsFiltersByNewsType() throws Exception {
        NewsCategory activityCategory = newsCategoryRepository.save(
                NewsCategory.create(NewsCategoryCode.RECRUITMENT_PARTICIPATION)
        );
        NewsCategory localCategory = newsCategoryRepository.save(
                NewsCategory.create(NewsCategoryCode.LOCAL_NEWS)
        );
        NewsSource source = newsSourceRepository.save(source());
        News activityNews = newsRepository.save(news(
                activityCategory,
                source,
                null,
                "activity-news-type",
                "활동소식",
                LocalDateTime.of(2026, 7, 2, 10, 0),
                RecruitmentStatus.OPEN
        ));
        News localNews = newsRepository.save(news(
                localCategory,
                source,
                null,
                "local-news-type",
                "지역소식",
                LocalDateTime.of(2026, 7, 1, 10, 0),
                RecruitmentStatus.OPEN
        ));

        mockMvc.perform(get("/api/v1/news").param("newsType", "ACTIVITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.items[0].newsId").value(activityNews.getId()));

        mockMvc.perform(get("/api/v1/news").param("newsType", "LOCAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.items[0].newsId").value(localNews.getId()));
    }

    @Test
    void getNewsFiltersAllCitiesByRegionLevel1() throws Exception {
        Region suwon = regionRepository.save(Region.create("경기도", "수원시"));
        Region guri = regionRepository.save(Region.create("경기도", "구리시"));
        Region seongnam = regionRepository.save(Region.create("경기도", "성남시"));
        Region seoul = regionRepository.save(Region.create("서울특별시", "강남구"));
        NewsCategory category = newsCategoryRepository.save(
                NewsCategory.create(NewsCategoryCode.BENEFIT_WELFARE_SERVICE)
        );
        NewsSource source = newsSourceRepository.save(source());
        News suwonNews = newsRepository.save(news(
                category,
                source,
                suwon.getId(),
                "suwon-region-level-1",
                "수원 소식",
                LocalDateTime.of(2026, 7, 1, 10, 0),
                null
        ));
        News guriNews = newsRepository.save(news(
                category,
                source,
                guri.getId(),
                "guri-region-level-1",
                "구리 소식",
                LocalDateTime.of(2026, 7, 2, 10, 0),
                null
        ));
        News seongnamNews = newsRepository.save(news(
                category,
                source,
                seongnam.getId(),
                "seongnam-region-level-1",
                "성남 소식",
                LocalDateTime.of(2026, 7, 3, 10, 0),
                null
        ));
        newsRepository.save(news(
                category,
                source,
                seoul.getId(),
                "seoul-region-level-1",
                "서울 소식",
                LocalDateTime.of(2026, 7, 4, 10, 0),
                null
        ));

        mockMvc.perform(get("/api/v1/news")
                        .param("regionLevel1", "경기도"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(3))
                .andExpect(jsonPath("$.result.items[0].newsId").value(seongnamNews.getId()))
                .andExpect(jsonPath("$.result.items[1].newsId").value(guriNews.getId()))
                .andExpect(jsonPath("$.result.items[2].newsId").value(suwonNews.getId()));
    }

    @Test
    void getNewsDetailReturnsContentAndIncreasesViewCount() throws Exception {
        Region region = regionRepository.save(Region.create("경기도", "수원시"));
        NewsCategory category = newsCategoryRepository.save(
                NewsCategory.create(NewsCategoryCode.LOCAL_NEWS)
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

        mockMvc.perform(get("/api/v1/news/{newsId}", news.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.newsId").value(news.getId()))
                .andExpect(jsonPath("$.result.title").value("상세 소식"))
                .andExpect(jsonPath("$.result.region").value("경기도 수원시"))
                .andExpect(jsonPath("$.result.categoryCode").value("LOCAL_NEWS"))
                .andExpect(jsonPath("$.result.categoryLabel").value("소식"))
                .andExpect(jsonPath("$.result.publishedAt").value("2026-07-02"))
                .andExpect(jsonPath("$.result.viewCount").value(1))
                .andExpect(jsonPath("$.result.scrapped").value(false));

        assertThat(newsRepository.findById(news.getId()).orElseThrow().getViewCount()).isEqualTo(1L);
    }

    @Test
    void getNewsDetailReturnsCommonNotFoundResponse() throws Exception {
        mockMvc.perform(get("/api/v1/news/{newsId}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON404_1"))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    void getRelatedRecruitingNewsReturnsLatestOpenNewsFromSameRegion() throws Exception {
        Region suwonYeongtong = regionRepository.save(
                Region.create("경기도", "수원시 영통구")
        );
        Region suwonPaldal = regionRepository.save(
                Region.create("경기도", "수원시 팔달구")
        );
        Region yongin = regionRepository.save(Region.create("경기도", "용인시"));
        NewsCategory category = newsCategoryRepository.save(
                NewsCategory.create(NewsCategoryCode.BENEFIT_WELFARE_SERVICE)
        );
        NewsSource source = newsSourceRepository.save(source());
        News current = newsRepository.save(news(
                category,
                source,
                suwonYeongtong.getId(),
                "related-current",
                "현재 조회 중인 소식",
                LocalDateTime.of(2026, 7, 10, 10, 0),
                RecruitmentStatus.OPEN
        ));
        News older = newsRepository.save(news(
                category,
                source,
                suwonPaldal.getId(),
                "related-older",
                "수원 이전 모집 소식",
                LocalDateTime.of(2026, 7, 1, 10, 0),
                RecruitmentStatus.OPEN
        ));
        News latest = news(
                category,
                source,
                suwonPaldal.getId(),
                "related-latest",
                "수원 최신 모집 소식",
                LocalDateTime.of(2026, 7, 3, 10, 0),
                RecruitmentStatus.OPEN
        );
        latest.increaseScrapCount();
        latest.increaseViewCount();
        latest.increaseViewCount();
        latest = newsRepository.save(latest);
        newsRepository.save(news(
                category,
                source,
                suwonPaldal.getId(),
                "related-over-limit",
                "수원 오래된 모집 소식",
                LocalDateTime.of(2026, 6, 30, 10, 0),
                RecruitmentStatus.OPEN
        ));
        newsRepository.save(news(
                category,
                source,
                suwonPaldal.getId(),
                "related-closed",
                "수원 마감 소식",
                LocalDateTime.of(2026, 7, 5, 10, 0),
                RecruitmentStatus.CLOSED
        ));
        newsRepository.save(news(
                category,
                source,
                yongin.getId(),
                "related-other-region",
                "용인 모집 소식",
                LocalDateTime.of(2026, 7, 6, 10, 0),
                RecruitmentStatus.OPEN
        ));

        mockMvc.perform(get("/api/v1/news/{newsId}/related", current.getId())
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200_1"))
                .andExpect(jsonPath("$.result.length()").value(2))
                .andExpect(jsonPath("$.result[0].newsId").value(latest.getId()))
                .andExpect(jsonPath("$.result[0].region").value("경기도 수원시 팔달구"))
                .andExpect(jsonPath("$.result[0].title").value("수원 최신 모집 소식"))
                .andExpect(jsonPath("$.result[0].scrapCount").value(1))
                .andExpect(jsonPath("$.result[0].viewCount").value(2))
                .andExpect(jsonPath("$.result[1].newsId").value(older.getId()));
    }

    @Test
    void getRelatedRecruitingNewsUsesDefaultSizeFive() throws Exception {
        Region currentRegion = regionRepository.save(
                Region.create("부산광역시", "수영구")
        );
        Region relatedRegion = regionRepository.save(
                Region.create("부산광역시", "해운대구")
        );
        NewsCategory category = newsCategoryRepository.save(
                NewsCategory.create(NewsCategoryCode.BENEFIT_WELFARE_SERVICE)
        );
        NewsSource source = newsSourceRepository.save(source());
        News current = newsRepository.save(news(
                category,
                source,
                currentRegion.getId(),
                "default-size-current",
                "현재 소식",
                LocalDateTime.of(2026, 7, 10, 10, 0),
                RecruitmentStatus.OPEN
        ));
        for (int index = 0; index < 6; index++) {
            newsRepository.save(news(
                    category,
                    source,
                    relatedRegion.getId(),
                    "default-size-" + index,
                    "관련 소식 " + index,
                    LocalDateTime.of(2026, 7, index + 1, 10, 0),
                    RecruitmentStatus.OPEN
            ));
        }

        mockMvc.perform(get("/api/v1/news/{newsId}/related", current.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(5));
    }

    @Test
    void getRelatedRecruitingNewsReturnsEmptyWhenCurrentNewsHasNoRegion() throws Exception {
        NewsCategory category = newsCategoryRepository.save(
                NewsCategory.create(NewsCategoryCode.BENEFIT_WELFARE_SERVICE)
        );
        NewsSource source = newsSourceRepository.save(source());
        News current = newsRepository.save(news(
                category,
                source,
                null,
                "related-no-region",
                "지역 없는 소식",
                LocalDateTime.of(2026, 7, 1, 10, 0),
                RecruitmentStatus.OPEN
        ));

        mockMvc.perform(get("/api/v1/news/{newsId}/related", current.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result").isEmpty());
    }

    @Test
    void getRelatedRecruitingNewsReturnsNotFoundForUnknownNews() throws Exception {
        mockMvc.perform(get("/api/v1/news/{newsId}/related", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON404_1"));
    }

    @Test
    void getRelatedRecruitingNewsRejectsInvalidSize() throws Exception {
        mockMvc.perform(get("/api/v1/news/{newsId}/related", 1L)
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));
    }

    @Test
    void getNewsRejectsInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/v1/news").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));
    }

    @Test
    void getNewsRejectsSortOtherThanViewOrScrap() throws Exception {
        mockMvc.perform(get("/api/v1/news").param("sort", "latest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));

        mockMvc.perform(get("/api/v1/news").param("sort", "review"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));
    }

    @Test
    void getNewsRejectsUnknownCategoryCode() throws Exception {
        mockMvc.perform(get("/api/v1/news").param("category", "교육재활"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));
    }

    @Test
    void getNewsRejectsUnknownNewsType() throws Exception {
        mockMvc.perform(get("/api/v1/news").param("newsType", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));
    }

    @Test
    void searchNewsMatchesTitleContentSourceNameAndRegionAndSortsByViewCountByDefault() throws Exception {
        Region seoul = regionRepository.save(Region.create("서울특별시", "강남구"));
        Region suwon = regionRepository.save(Region.create("경기도", "수원시"));
        NewsCategory volunteer = newsCategoryRepository.save(
                NewsCategory.create(NewsCategoryCode.RECRUITMENT_PARTICIPATION)
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
        titleMatch.increaseViewCount();
        contentMatch.increaseViewCount();
        contentMatch.increaseViewCount();
        sourceMatch.increaseViewCount();
        sourceMatch.increaseViewCount();
        sourceMatch.increaseViewCount();

        mockMvc.perform(get("/api/v1/news/search")
                        .param("keyword", "봉사")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(3))
                .andExpect(jsonPath("$.result.items[0].newsId").value(sourceMatch.getId()))
                .andExpect(jsonPath("$.result.items[1].newsId").value(contentMatch.getId()))
                .andExpect(jsonPath("$.result.items[2].newsId").value(titleMatch.getId()));

        mockMvc.perform(get("/api/v1/news/search")
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
                NewsCategory.create(NewsCategoryCode.RECRUITMENT_PARTICIPATION)
        );
        NewsCategory support = newsCategoryRepository.save(
                NewsCategory.create(NewsCategoryCode.LOCAL_NEWS)
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

        mockMvc.perform(get("/api/v1/news/search")
                        .param("keyword", "봉사")
                        .param("regionId", seoul.getId().toString())
                        .param("category", "RECRUITMENT_PARTICIPATION")
                        .param("status", "RECRUITING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.items[0].newsId").value(expected.getId()));
    }

    @Test
    void searchNewsFiltersByNewsType() throws Exception {
        NewsCategory activityCategory = newsCategoryRepository.save(
                NewsCategory.create(NewsCategoryCode.EDUCATION_SEMINAR)
        );
        NewsCategory localCategory = newsCategoryRepository.save(
                NewsCategory.create(NewsCategoryCode.LOCAL_POLICY)
        );
        NewsSource source = newsSourceRepository.save(source());
        newsRepository.save(news(
                activityCategory,
                source,
                null,
                "activity-search-news-type",
                "발달 교육 프로그램",
                LocalDateTime.of(2026, 7, 2, 10, 0),
                RecruitmentStatus.OPEN
        ));
        News expected = newsRepository.save(news(
                localCategory,
                source,
                null,
                "local-search-news-type",
                "발달 지원 정책",
                LocalDateTime.of(2026, 7, 1, 10, 0),
                RecruitmentStatus.OPEN
        ));

        mockMvc.perform(get("/api/v1/news/search")
                        .param("keyword", "발달")
                        .param("newsType", "LOCAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.items[0].newsId").value(expected.getId()));
    }

    @Test
    void searchNewsSortsByScrapCount() throws Exception {
        NewsCategory category = newsCategoryRepository.save(
                NewsCategory.create(NewsCategoryCode.RECRUITMENT_PARTICIPATION)
        );
        NewsSource source = newsSourceRepository.save(source());
        News lessScrapped = newsRepository.save(news(
                category,
                source,
                null,
                "search-less-scrapped",
                "발달 프로그램 안내",
                LocalDateTime.of(2026, 7, 2, 10, 0),
                RecruitmentStatus.OPEN
        ));
        News moreScrapped = newsRepository.save(news(
                category,
                source,
                null,
                "search-more-scrapped",
                "발달 프로그램 모집",
                LocalDateTime.of(2026, 7, 1, 10, 0),
                RecruitmentStatus.OPEN
        ));
        lessScrapped.increaseScrapCount();
        moreScrapped.increaseScrapCount();
        moreScrapped.increaseScrapCount();

        mockMvc.perform(get("/api/v1/news/search")
                        .param("keyword", "발달")
                        .param("sort", "SCRAP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].newsId").value(moreScrapped.getId()))
                .andExpect(jsonPath("$.result.items[1].newsId").value(lessScrapped.getId()));
    }

    @Test
    void searchNewsRejectsBlankKeyword() throws Exception {
        mockMvc.perform(get("/api/v1/news/search").param("keyword", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));
    }

    @Test
    void getNewsSearchSuggestionsPrioritizesPrefixAndReturnsDistinctVisibleTitles() throws Exception {
        NewsCategory category = newsCategoryRepository.save(
                NewsCategory.create(NewsCategoryCode.RECRUITMENT_PARTICIPATION)
        );
        NewsSource source = newsSourceRepository.save(source());
        newsRepository.save(news(
                category,
                source,
                null,
                "suggestion-prefix-1",
                "봉사활동 참여자 모집",
                LocalDateTime.of(2026, 7, 1, 10, 0),
                RecruitmentStatus.OPEN
        ));
        newsRepository.save(news(
                category,
                source,
                null,
                "suggestion-prefix-duplicate",
                "봉사활동 참여자 모집",
                LocalDateTime.of(2026, 7, 2, 10, 0),
                RecruitmentStatus.OPEN
        ));
        newsRepository.save(news(
                category,
                source,
                null,
                "suggestion-contains",
                "청소년 봉사단 모집",
                LocalDateTime.of(2026, 7, 3, 10, 0),
                RecruitmentStatus.OPEN
        ));
        newsRepository.save(news(
                category,
                source,
                null,
                "suggestion-content-only",
                "언어치료 프로그램",
                "프로그램 안내",
                "봉사 활동이 포함된 본문",
                "테스트 기관",
                LocalDateTime.of(2026, 7, 4, 10, 0),
                RecruitmentStatus.OPEN
        ));
        News deleted = newsRepository.save(news(
                category,
                source,
                null,
                "suggestion-deleted",
                "봉사 종료 소식",
                LocalDateTime.of(2026, 7, 5, 10, 0),
                RecruitmentStatus.CLOSED
        ));
        deleted.delete();

        mockMvc.perform(get("/api/v1/news/search/suggestions")
                        .param("keyword", " 봉사 ")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200_1"))
                .andExpect(jsonPath("$.result.suggestions.length()").value(2))
                .andExpect(jsonPath("$.result.suggestions[0].text")
                        .value("봉사활동 참여자 모집"))
                .andExpect(jsonPath("$.result.suggestions[0].type").value("NEWS_TITLE"))
                .andExpect(jsonPath("$.result.suggestions[1].text")
                        .value("청소년 봉사단 모집"))
                .andExpect(jsonPath("$.result.suggestions[1].type").value("NEWS_TITLE"));
    }

    @Test
    void getNewsSearchSuggestionsUsesDefaultSizeTen() throws Exception {
        NewsCategory category = newsCategoryRepository.save(
                NewsCategory.create(NewsCategoryCode.RECRUITMENT_PARTICIPATION)
        );
        NewsSource source = newsSourceRepository.save(source());
        for (int index = 0; index < 11; index++) {
            newsRepository.save(news(
                    category,
                    source,
                    null,
                    "suggestion-default-size-" + index,
                    "봉사 프로그램 " + String.format("%02d", index),
                    LocalDateTime.of(2026, 7, index + 1, 10, 0),
                    RecruitmentStatus.OPEN
            ));
        }

        mockMvc.perform(get("/api/v1/news/search/suggestions")
                        .param("keyword", "봉사"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.suggestions.length()").value(10));
    }

    @Test
    void getNewsSearchSuggestionsReturnsEmptyListWhenNoTitleMatches() throws Exception {
        mockMvc.perform(get("/api/v1/news/search/suggestions")
                        .param("keyword", "검색결과없음"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.suggestions").isArray())
                .andExpect(jsonPath("$.result.suggestions").isEmpty());
    }

    @Test
    void getNewsSearchSuggestionsRejectsInvalidRequest() throws Exception {
        mockMvc.perform(get("/api/v1/news/search/suggestions")
                        .param("keyword", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));

        mockMvc.perform(get("/api/v1/news/search/suggestions")
                        .param("keyword", "봉"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));

        mockMvc.perform(get("/api/v1/news/search/suggestions")
                        .param("keyword", " 봉 "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));

        mockMvc.perform(get("/api/v1/news/search/suggestions")
                        .param("keyword", "봉사")
                        .param("size", "21"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));
    }

    @Test
    void toggleNewsScrapAddsAndRemovesScrap() throws Exception {
        NewsCategory category = newsCategoryRepository.save(
                NewsCategory.create(NewsCategoryCode.LOCAL_NEWS)
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

        mockMvc.perform(post("/api/v1/news/{newsId}/scrap", news.getId())
                        .with(authenticatedUser(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200_1"))
                .andExpect(jsonPath("$.result.newsId").value(news.getId()))
                .andExpect(jsonPath("$.result.scrapped").value(true))
                .andExpect(jsonPath("$.result.scrapCount").value(1));

        assertThat(newsScrapRepository.existsByNewsIdAndUserId(news.getId(), 10L)).isTrue();
        assertThat(newsRepository.findById(news.getId()).orElseThrow().getScrapCount()).isOne();

        mockMvc.perform(post("/api/v1/news/{newsId}/scrap", news.getId())
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
        mockMvc.perform(post("/api/v1/news/{newsId}/scrap", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void toggleNewsScrapReturnsCommonNotFoundResponse() throws Exception {
        mockMvc.perform(post("/api/v1/news/{newsId}/scrap", 999999L)
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
                category.getCode(),
                category.getNewsType(),
                status
        );
        return News.create(category, source, regionId, candidate);
    }
}
