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
class GuriDisabledWelfareProgramNewsCollectorTest {

    @Mock
    private OdcloudClient odcloudClient;

    @Test
    void mapsGuriWelfareProgramToNewsCandidateWithoutInventingRecruitmentDates() {
        when(odcloudClient.fetchPage(
                GuriDisabledWelfareProgramNewsCollector.API_RESOURCE_PATH,
                1,
                100
        )).thenReturn(new OdcloudPageResponse(
                1,
                100,
                1,
                1,
                List.of(Map.<String, Object>ofEntries(
                        Map.entry("시군명", "구리시"),
                        Map.entry("복지관명", "구리시장애인종합복지관"),
                        Map.entry("이용대상", "지역주민"),
                        Map.entry("이용대상상세조건(장애유형)", "제한 없음"),
                        Map.entry("구분", "교육재활"),
                        Map.entry("프로그램명", "장애바로알기교육"),
                        Map.entry("프로그램내용", "지역주민 대상 장애인식개선 교육"),
                        Map.entry("이용시간", "월 1회"),
                        Map.entry("이용금액", 0),
                        Map.entry("소재지도로명주소", "경기도 구리시 이문안로 86-1"),
                        Map.entry("대표전화번호", "031-562-0068"),
                        Map.entry("위도", "37.59152098"),
                        Map.entry("경도", "127.1410639"),
                        Map.entry("데이터기준일자", "2026-03-30")
                ))
        ));
        NewsSource source = NewsSource.create(
                NewsSourceType.PUBLIC_API,
                "테스트 출처",
                "https://api.odcloud.kr/api",
                "https://www.data.go.kr"
        );
        GuriDisabledWelfareProgramNewsCollector collector =
                new GuriDisabledWelfareProgramNewsCollector(odcloudClient);

        List<NewsCandidate> result = collector.collect(source);

        assertThat(result).singleElement().satisfies(candidate -> {
            assertThat(candidate.title()).isEqualTo("장애바로알기교육");
            assertThat(candidate.summary()).isEqualTo("지역주민 대상 장애인식개선 교육");
            assertThat(candidate.sourceName()).isEqualTo("구리시장애인종합복지관");
            assertThat(candidate.regionName()).isEqualTo("경기도 구리시");
            assertThat(candidate.categoryCode()).isEqualTo(NewsCategoryCode.EDUCATION_SEMINAR);
            assertThat(candidate.newsType()).isEqualTo(NewsType.ACTIVITY);
            assertThat(candidate.recruitmentStatus()).isNull();
            assertThat(candidate.programStartDate()).isNull();
            assertThat(candidate.programEndDate()).isNull();
            assertThat(candidate.applyStartDate()).isNull();
            assertThat(candidate.applyEndDate()).isNull();
            assertThat(candidate.publishedAt().toLocalDate()).isEqualTo(LocalDate.of(2026, 3, 30));
            assertThat(candidate.targetAudience()).contains("지역주민", "제한 없음");
            assertThat(candidate.content()).contains(
                    "프로그램 내용",
                    "이용 금액: 무료",
                    "경기도 구리시 이문안로 86-1"
            );
            assertThat(candidate.externalItemId()).startsWith("guri-");
        });
        verify(odcloudClient).fetchPage(
                GuriDisabledWelfareProgramNewsCollector.API_RESOURCE_PATH,
                1,
                100
        );
    }
}
