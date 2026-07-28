package com.bodeum.domain.news.infrastructure.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeum.domain.news.collector.NewsCandidate;
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
class SeongnamDisabledWelfareProgramNewsCollectorTest {

    @Mock
    private OdcloudClient odcloudClient;

    @Test
    void mapsSeongnamWelfareProgramWithDatasetSpecificColumnNames() {
        when(odcloudClient.fetchPage(
                SeongnamDisabledWelfareProgramNewsCollector.API_RESOURCE_PATH,
                1,
                100
        )).thenReturn(new OdcloudPageResponse(
                1,
                100,
                1,
                1,
                List.of(Map.<String, Object>ofEntries(
                        Map.entry("시군명", "성남시"),
                        Map.entry("복지관명", "성남시장애인종합복지관"),
                        Map.entry("이용대상", "장애인"),
                        Map.entry(
                                "이용대상상세조건(장애유형)",
                                "의사소통에 어려움을 보이는 자"
                        ),
                        Map.entry("이용대상상세조건(연령제한)", "만2세~만65세"),
                        Map.entry(
                                "이용대상상세조건(기타조건)",
                                "성남시 거주 장애인 및 미등록 아동"
                        ),
                        Map.entry("구분", "의료재활"),
                        Map.entry("상세구분", "언어치료"),
                        Map.entry("프로그램명", "언어치료"),
                        Map.entry("프로그램내용", "개별 언어치료 진행"),
                        Map.entry("이용시간", "연중"),
                        Map.entry("이용금액(원)", 10000),
                        Map.entry("이용금액산정기준", "1회"),
                        Map.entry("부가비용", 0),
                        Map.entry(
                                "소재지도로명주소",
                                "경기도 성남시 중원구 사기막골로150번길 20"
                        ),
                        Map.entry("전화번호", "031-733-3322"),
                        Map.entry("우편번호", 13203),
                        Map.entry("위도", "37.44343"),
                        Map.entry("경도", "127.1789"),
                        Map.entry("데이터기준일", "2026-05-18")
                ))
        ));
        NewsSource source = NewsSource.create(
                NewsSourceType.PUBLIC_API,
                "테스트 출처",
                "https://api.odcloud.kr/api",
                "https://www.data.go.kr"
        );
        SeongnamDisabledWelfareProgramNewsCollector collector =
                new SeongnamDisabledWelfareProgramNewsCollector(odcloudClient);

        List<NewsCandidate> result = collector.collect(source);

        assertThat(result).singleElement().satisfies(candidate -> {
            assertThat(candidate.title()).isEqualTo("언어치료");
            assertThat(candidate.summary()).isEqualTo("개별 언어치료 진행");
            assertThat(candidate.sourceName()).isEqualTo("성남시장애인종합복지관");
            assertThat(candidate.regionName()).isEqualTo("경기도 성남시");
            assertThat(candidate.categoryName()).isEqualTo("의료재활");
            assertThat(candidate.newsType()).isEqualTo(NewsType.ACTIVITY);
            assertThat(candidate.recruitmentStatus()).isNull();
            assertThat(candidate.programStartDate()).isNull();
            assertThat(candidate.programEndDate()).isNull();
            assertThat(candidate.applyStartDate()).isNull();
            assertThat(candidate.applyEndDate()).isNull();
            assertThat(candidate.publishedAt().toLocalDate()).isEqualTo(LocalDate.of(2026, 5, 18));
            assertThat(candidate.contact()).isEqualTo("031-733-3322");
            assertThat(candidate.targetAudience()).contains("만2세~만65세", "성남시 거주");
            assertThat(candidate.content()).contains(
                    "이용 금액: 10,000원",
                    "부가 비용: 없음",
                    "경기도 성남시 중원구 사기막골로150번길 20",
                    "데이터 기준일자: 2026-05-18"
            ).doesNotContain("지번 주소");
            assertThat(candidate.externalItemId()).startsWith("seongnam-");
        });
        verify(odcloudClient).fetchPage(
                SeongnamDisabledWelfareProgramNewsCollector.API_RESOURCE_PATH,
                1,
                100
        );
    }
}
