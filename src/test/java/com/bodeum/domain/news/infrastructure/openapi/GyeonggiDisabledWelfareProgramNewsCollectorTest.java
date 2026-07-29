package com.bodeum.domain.news.infrastructure.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeum.domain.news.collector.NewsCandidate;
import com.bodeum.domain.news.entity.NewsCategoryCode;
import com.bodeum.domain.news.entity.NewsSource;
import com.bodeum.domain.news.entity.NewsSourceType;
import com.bodeum.domain.news.entity.NewsType;
import com.bodeum.global.infrastructure.openapi.GgOpenApiClient;
import com.bodeum.global.infrastructure.openapi.GgOpenApiPageResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GyeonggiDisabledWelfareProgramNewsCollectorTest {

    @Mock
    private GgOpenApiClient ggOpenApiClient;

    @Test
    void mapsGyeonggiProgramAndExcludesCitiesHandledByIndividualCollectors() {
        when(ggOpenApiClient.fetchPage(
                GyeonggiDisabledWelfareProgramNewsCollector.API_RESOURCE_PATH,
                GyeonggiDisabledWelfareProgramNewsCollector.API_RESPONSE_KEY,
                1,
                100
        )).thenReturn(new GgOpenApiPageResponse(
                2,
                List.of(
                        programRow("가평군", "가평군장애인복지관", "자립생활 프로그램"),
                        programRow("수원시", "수원시장애인종합복지관", "개별 수집 대상")
                )
        ));
        GyeonggiDisabledWelfareProgramNewsCollector collector =
                new GyeonggiDisabledWelfareProgramNewsCollector(ggOpenApiClient);

        List<NewsCandidate> result = collector.collect(newsSource());

        assertThat(result).singleElement().satisfies(candidate -> {
            assertThat(candidate.title()).isEqualTo("자립생활 프로그램");
            assertThat(candidate.summary()).isEqualTo("지역사회 자립을 위한 교육");
            assertThat(candidate.sourceName()).isEqualTo("가평군장애인복지관");
            assertThat(candidate.regionName()).isEqualTo("경기도 가평군");
            assertThat(candidate.categoryCode())
                    .isEqualTo(NewsCategoryCode.BENEFIT_WELFARE_SERVICE);
            assertThat(candidate.newsType()).isEqualTo(NewsType.ACTIVITY);
            assertThat(candidate.recruitmentStatus()).isNull();
            assertThat(candidate.publishedAt().toLocalDate()).isEqualTo(LocalDate.of(2026, 5, 20));
            assertThat(candidate.targetAudience()).contains("성인 장애인", "지체장애");
            assertThat(candidate.contact()).isEqualTo("031-000-0000");
            assertThat(candidate.content()).contains(
                    "이용 금액: 25,000원",
                    "부가 비용: 없음",
                    "추가 연락처: 031-000-0001",
                    "유의사항: 사전 문의 필요"
            );
            assertThat(candidate.externalItemId()).startsWith("gg-welfare-program-");
        });
        assertThat(collector.sourceApiBaseUrl()).isEqualTo("https://openapi.gg.go.kr");
        verify(ggOpenApiClient).fetchPage(
                GyeonggiDisabledWelfareProgramNewsCollector.API_RESOURCE_PATH,
                GyeonggiDisabledWelfareProgramNewsCollector.API_RESPONSE_KEY,
                1,
                100
        );
    }

    @Test
    void collectsEveryPageAndCollapsesDuplicateExternalItems() {
        Map<String, Object> firstPageRow = programRow(
                "가평군",
                "가평군장애인복지관",
                "자립생활 프로그램"
        );
        Map<String, Object> secondPageRow = new java.util.HashMap<>(firstPageRow);
        secondPageRow.put("PROG_CONT", "수정된 프로그램 내용");
        when(ggOpenApiClient.fetchPage(
                GyeonggiDisabledWelfareProgramNewsCollector.API_RESOURCE_PATH,
                GyeonggiDisabledWelfareProgramNewsCollector.API_RESPONSE_KEY,
                1,
                100
        )).thenReturn(new GgOpenApiPageResponse(101, List.of(firstPageRow)));
        when(ggOpenApiClient.fetchPage(
                GyeonggiDisabledWelfareProgramNewsCollector.API_RESOURCE_PATH,
                GyeonggiDisabledWelfareProgramNewsCollector.API_RESPONSE_KEY,
                2,
                100
        )).thenReturn(new GgOpenApiPageResponse(101, List.of(secondPageRow)));
        GyeonggiDisabledWelfareProgramNewsCollector collector =
                new GyeonggiDisabledWelfareProgramNewsCollector(ggOpenApiClient);

        List<NewsCandidate> result = collector.collect(newsSource());

        assertThat(result).singleElement()
                .extracting(NewsCandidate::summary)
                .isEqualTo("수정된 프로그램 내용");
        verify(ggOpenApiClient).fetchPage(
                GyeonggiDisabledWelfareProgramNewsCollector.API_RESOURCE_PATH,
                GyeonggiDisabledWelfareProgramNewsCollector.API_RESPONSE_KEY,
                2,
                100
        );
    }

    private Map<String, Object> programRow(String sigungu, String facility, String title) {
        return Map.ofEntries(
                Map.entry("SIGUN_NM", sigungu),
                Map.entry("SIGUN_CD", "41820"),
                Map.entry("CMWELFCT_NM_INFO", facility),
                Map.entry("USE_TARGET", "성인 장애인"),
                Map.entry("USE_TARGET_OBSTCL_TYPE_COND", "지체장애"),
                Map.entry("USE_TARGET_AGE_LIMITN_COND", "만 18세 이상"),
                Map.entry("PROG_DIV_NM", "자립지원"),
                Map.entry("DETAIL_DIV_NM", "일상생활"),
                Map.entry("PROG_TITLE", title),
                Map.entry("PROG_CONT", "지역사회 자립을 위한 교육"),
                Map.entry("USE_TM_INFO", "매주 화요일"),
                Map.entry("USE_AMT", 25000),
                Map.entry("USE_AMT_CALC_STD_INFO", "월 기준"),
                Map.entry("ADDITN_EXPN_INFO", 0),
                Map.entry("REFINE_ROADNM_ADDR", "경기도 가평군 가평읍 중앙로 1"),
                Map.entry("REFINE_LOTNO_ADDR", "경기도 가평군 가평읍 1"),
                Map.entry("TELNO", "031-000-0000"),
                Map.entry("ADD_CONTCT_NO_INFO", "031-000-0001"),
                Map.entry("REFINE_ZIPNO", "12400"),
                Map.entry("REFINE_WGS84_LAT", "37.8315"),
                Map.entry("REFINE_WGS84_LOGT", "127.5095"),
                Map.entry("ATENTN_MATR", "사전 문의 필요"),
                Map.entry("DATA_STD_DE", "2026-05-20")
        );
    }

    private NewsSource newsSource() {
        return NewsSource.create(
                NewsSourceType.PUBLIC_API,
                GyeonggiDisabledWelfareProgramNewsCollector.SOURCE_NAME,
                "https://openapi.gg.go.kr",
                GyeonggiDisabledWelfareProgramNewsCollector.DATA_PORTAL_URL
        );
    }
}
