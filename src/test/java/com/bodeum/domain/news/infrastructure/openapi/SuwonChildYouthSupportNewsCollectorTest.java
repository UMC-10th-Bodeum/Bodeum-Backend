package com.bodeum.domain.news.infrastructure.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeum.domain.news.collector.NewsCandidate;
import com.bodeum.domain.news.entity.NewsCategoryCode;
import com.bodeum.domain.news.entity.NewsSource;
import com.bodeum.domain.news.entity.NewsSourceType;
import com.bodeum.domain.news.entity.NewsType;
import com.bodeum.global.infrastructure.openapi.OdcloudClient;
import com.bodeum.global.infrastructure.openapi.OdcloudPageResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SuwonChildYouthSupportNewsCollectorTest {

    @Mock
    private OdcloudClient odcloudClient;

    @Test
    void mapsSuwonProviderDataToNewsCandidate() {
        when(odcloudClient.fetchPage(
                SuwonChildYouthSupportNewsCollector.API_RESOURCE_PATH,
                1,
                100
        )).thenReturn(new OdcloudPageResponse(
                1,
                100,
                1,
                1,
                List.of(Map.<String, Object>ofEntries(
                        Map.entry("시도명", "경기도"),
                        Map.entry("시군명", "수원시"),
                        Map.entry("지정기관명", "수원아동발달문화센터"),
                        Map.entry("지정시작일", "2024-01-01"),
                        Map.entry("지정종료일", "2099-12-31"),
                        Map.entry("도로명주소", "경기도 수원시 장안구 경수대로 1022"),
                        Map.entry("지번주소", "경기도 수원시 장안구 파장동 627-6"),
                        Map.entry("연락처", "031-253-9098"),
                        Map.entry("서비스대상", "본인"),
                        Map.entry("제공서비스", "발달"),
                        Map.entry("데이터기준일자", "2026-04-07")
                ))
        ));
        NewsSource source = NewsSource.create(
                NewsSourceType.PUBLIC_API,
                "테스트 출처",
                "https://api.odcloud.kr/api",
                "https://www.data.go.kr"
        );
        SuwonChildYouthSupportNewsCollector collector =
                new SuwonChildYouthSupportNewsCollector(odcloudClient);

        List<NewsCandidate> result = collector.collect(source);

        assertThat(result).singleElement().satisfies(candidate -> {
            assertThat(candidate.title()).isEqualTo("수원아동발달문화센터 발달 지원 서비스");
            assertThat(candidate.regionName()).isEqualTo("경기도 수원시");
            assertThat(candidate.categoryCode()).isEqualTo(NewsCategoryCode.LOCAL_NEWS);
            assertThat(candidate.newsType()).isEqualTo(NewsType.LOCAL);
            assertThat(candidate.recruitmentStatus()).isNull();
            assertThat(candidate.programStartDate()).isNull();
            assertThat(candidate.programEndDate()).isNull();
            assertThat(candidate.applyStartDate()).isNull();
            assertThat(candidate.applyEndDate()).isNull();
            assertThat(candidate.sourceName()).isEqualTo("수원아동발달문화센터");
            assertThat(candidate.publishedAt().toLocalDate()).isEqualTo(LocalDate.of(2026, 4, 7));
            assertThat(candidate.content()).contains("도로명 주소", "031-253-9098", "지정 시작일");
            assertThat(candidate.externalItemId()).startsWith("suwon-");
        });
        verify(odcloudClient).fetchPage(
                SuwonChildYouthSupportNewsCollector.API_RESOURCE_PATH,
                1,
                100
        );
    }
}
