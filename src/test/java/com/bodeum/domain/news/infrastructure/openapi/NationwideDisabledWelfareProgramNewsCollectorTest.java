package com.bodeum.domain.news.infrastructure.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeum.domain.news.collector.NewsCandidate;
import com.bodeum.domain.news.entity.NewsCategoryCode;
import com.bodeum.domain.news.entity.NewsSource;
import com.bodeum.domain.news.entity.NewsSourceType;
import com.bodeum.domain.news.entity.NewsType;
import com.bodeum.global.infrastructure.openapi.DataGoOpenApiClient;
import com.bodeum.global.infrastructure.openapi.DataGoOpenApiPageResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NationwideDisabledWelfareProgramNewsCollectorTest {

    @Mock
    private DataGoOpenApiClient dataGoOpenApiClient;

    @Test
    void mapsStandardProgramAndExcludesRegionsHandledByDedicatedCollectors() {
        when(dataGoOpenApiClient.fetchPage(
                NationwideDisabledWelfareProgramNewsCollector.API_RESOURCE_PATH,
                1,
                500
        )).thenReturn(new DataGoOpenApiPageResponse(
                2,
                List.of(
                        programRow("부산광역시", "금정구", "예술누림 무빙아트"),
                        programRow("경기도", "수원시", "전용 수집 대상")
                )
        ));
        NationwideDisabledWelfareProgramNewsCollector collector =
                new NationwideDisabledWelfareProgramNewsCollector(dataGoOpenApiClient);

        List<NewsCandidate> result = collector.collect(newsSource());

        assertThat(result).singleElement().satisfies(candidate -> {
            assertThat(candidate.title()).isEqualTo("예술누림 무빙아트");
            assertThat(candidate.summary()).isEqualTo("미술 활동 프로그램");
            assertThat(candidate.sourceName()).isEqualTo("금정구장애인복지관");
            assertThat(candidate.regionName()).isEqualTo("부산광역시 금정구");
            assertThat(candidate.categoryCode())
                    .isEqualTo(NewsCategoryCode.BENEFIT_WELFARE_SERVICE);
            assertThat(candidate.newsType()).isEqualTo(NewsType.ACTIVITY);
            assertThat(candidate.recruitmentStatus()).isNull();
            assertThat(candidate.publishedAt().toLocalDate()).isEqualTo(LocalDate.of(2025, 12, 23));
            assertThat(candidate.targetAudience()).contains("성인 장애인", "사전 상담 필요");
            assertThat(candidate.contact()).isEqualTo("051-000-0000");
            assertThat(candidate.manager()).isEqualTo("부산광역시 금정구청");
            assertThat(candidate.content()).contains(
                    "이용 세부 내용: 주 1회 미술 활동",
                    "이용 시간: 09:00 ~ 11:00",
                    "이용 금액: 10,000원",
                    "부가 비용: 없음",
                    "관리 기관: 부산광역시 금정구청",
                    "관리 기관 전화번호"
            );
            assertThat(candidate.externalItemId()).startsWith("nationwide-welfare-program-");
        });
        assertThat(collector.sourceApiBaseUrl()).isEqualTo("https://api.data.go.kr/openapi");
        verify(dataGoOpenApiClient).fetchPage(
                NationwideDisabledWelfareProgramNewsCollector.API_RESOURCE_PATH,
                1,
                500
        );
    }

    @Test
    void collectsEveryPageAndCollapsesDuplicateExternalItems() {
        Map<String, Object> firstPageRow = programRow(
                "부산광역시",
                "금정구",
                "예술누림 무빙아트"
        );
        Map<String, Object> secondPageRow = new java.util.HashMap<>(firstPageRow);
        secondPageRow.put("prgrmCn", "수정된 프로그램 내용");
        when(dataGoOpenApiClient.fetchPage(
                NationwideDisabledWelfareProgramNewsCollector.API_RESOURCE_PATH,
                1,
                500
        )).thenReturn(new DataGoOpenApiPageResponse(501, List.of(firstPageRow)));
        when(dataGoOpenApiClient.fetchPage(
                NationwideDisabledWelfareProgramNewsCollector.API_RESOURCE_PATH,
                2,
                500
        )).thenReturn(new DataGoOpenApiPageResponse(501, List.of(secondPageRow)));
        NationwideDisabledWelfareProgramNewsCollector collector =
                new NationwideDisabledWelfareProgramNewsCollector(dataGoOpenApiClient);

        List<NewsCandidate> result = collector.collect(newsSource());

        assertThat(result).singleElement()
                .extracting(NewsCandidate::summary)
                .isEqualTo("수정된 프로그램 내용");
        verify(dataGoOpenApiClient).fetchPage(
                NationwideDisabledWelfareProgramNewsCollector.API_RESOURCE_PATH,
                2,
                500
        );
    }

    private Map<String, Object> programRow(String regionLevel1, String regionLevel2, String title) {
        return Map.ofEntries(
                Map.entry("prgrmNm", title),
                Map.entry("prgrmCn", "미술 활동 프로그램"),
                Map.entry("wlfcNm", "금정구장애인복지관"),
                Map.entry("ctpvNm", regionLevel1),
                Map.entry("sggNm", regionLevel2),
                Map.entry("lctnRoadNmAddr", "부산광역시 금정구 서부로 77"),
                Map.entry("lctnLotnoAddr", "부산광역시 금정구 서동 1"),
                Map.entry("lat", "35.2000"),
                Map.entry("lot", "129.1000"),
                Map.entry("utztnTrgtNm", "성인 장애인"),
                Map.entry("utztnTrgtDtlCndNm", "사전 상담 필요"),
                Map.entry("utztnDtlCn", "주 1회 미술 활동"),
                Map.entry("utztnBgngTm", "09:00"),
                Map.entry("utztnEndTm", "11:00"),
                Map.entry("utztnAmt", "10000"),
                Map.entry("sbsdCst", "0"),
                Map.entry("telno", "051-000-0000"),
                Map.entry("mngInstNm", "부산광역시 금정구청"),
                Map.entry("mngInstTelno", "051-000-0001"),
                Map.entry("dataCrtrYmd", "2025-12-23"),
                Map.entry("insttCode", "3350000")
        );
    }

    private NewsSource newsSource() {
        return NewsSource.create(
                NewsSourceType.PUBLIC_API,
                NationwideDisabledWelfareProgramNewsCollector.SOURCE_NAME,
                "https://api.data.go.kr/openapi",
                NationwideDisabledWelfareProgramNewsCollector.DATA_PORTAL_URL
        );
    }
}
