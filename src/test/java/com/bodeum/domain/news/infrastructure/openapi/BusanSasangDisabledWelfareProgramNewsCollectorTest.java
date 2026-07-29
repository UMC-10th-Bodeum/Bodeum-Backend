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
class BusanSasangDisabledWelfareProgramNewsCollectorTest {

    @Mock
    private OdcloudClient odcloudClient;

    @Test
    void mapsSasangWelfareProgramToNewsCandidate() {
        when(odcloudClient.fetchPage(
                BusanSasangDisabledWelfareProgramNewsCollector.API_RESOURCE_PATH,
                1,
                100
        )).thenReturn(new OdcloudPageResponse(
                1,
                100,
                1,
                1,
                List.of(Map.of(
                        "프로그램대상", "아동기",
                        "프로그램명", "언어치료",
                        "내용", "발달지연 아동의 언어발달지원",
                        "대상", "만2~13세 아동"
                ))
        ));
        NewsSource source = NewsSource.create(
                NewsSourceType.PUBLIC_API,
                "테스트 출처",
                "https://api.odcloud.kr/api",
                "https://www.data.go.kr"
        );
        BusanSasangDisabledWelfareProgramNewsCollector collector =
                new BusanSasangDisabledWelfareProgramNewsCollector(odcloudClient);

        List<NewsCandidate> result = collector.collect(source);

        assertThat(result).singleElement().satisfies(candidate -> {
            assertThat(candidate.title()).isEqualTo("언어치료");
            assertThat(candidate.summary()).isEqualTo("발달지연 아동의 언어발달지원");
            assertThat(candidate.sourceName()).isEqualTo("사상구장애인복지관");
            assertThat(candidate.regionName()).isEqualTo("부산광역시 사상구");
            assertThat(candidate.categoryCode())
                    .isEqualTo(NewsCategoryCode.BENEFIT_WELFARE_SERVICE);
            assertThat(candidate.newsType()).isEqualTo(NewsType.ACTIVITY);
            assertThat(candidate.publishedAt().toLocalDate())
                    .isEqualTo(LocalDate.of(2026, 7, 20));
            assertThat(candidate.targetAudience()).contains("아동기", "만2~13세 아동");
            assertThat(candidate.content()).contains(
                    "프로그램 내용: 발달지연 아동의 언어발달지원",
                    "운영 기관: 사상구장애인복지관",
                    "연령 조건: 만2~13세 아동"
            );
            assertThat(candidate.externalItemId()).startsWith("busan-sasang-");
        });
        verify(odcloudClient).fetchPage(
                BusanSasangDisabledWelfareProgramNewsCollector.API_RESOURCE_PATH,
                1,
                100
        );
    }
}
