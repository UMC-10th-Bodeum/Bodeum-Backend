package com.bodeum.domain.ai.service.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.info.entity.enums.InfoSubCategory;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiResourceListAnswerBuilderTest {

    private final AiResourceListAnswerBuilder builder = new AiResourceListAnswerBuilder();

    @Test
    void buildsExactResourceListWithoutInternalMetadata() {
        AiReferenceDocument first = document(
                1L, "이안아동발달연구소",
                "정보명: 이안아동발달연구소\n대분류: 기관\n세부 분류: 치료·재활기관\n"
                        + "주소: 경기도 수원시 영통구\n전화번호: 031-111-1111");
        AiReferenceDocument second = document(
                2L, "수원아동발달문화센터",
                "정보명: 수원아동발달문화센터\n주소: 경기도 수원시 장안구");

        String answer = builder.build(
                List.of(first, second), 5, false, InfoSubCategory.THERAPY_REHAB);

        assertThat(answer)
                .startsWith("요청하신 5곳 중 현재 보듬에서 확인한 "
                        + "치료·재활기관 2곳을 안내드립니다.")
                .contains("**이안아동발달연구소**")
                .contains("주소: 경기도 수원시 영통구")
                .contains("전화번호: 031-111-1111")
                .contains("**수원아동발달문화센터**")
                .doesNotContain("정보명:")
                .doesNotContain("대분류:")
                .doesNotContain("세부 분류:");
    }

    @Test
    void describesAdditionalResultsUsingActualCount() {
        String answer = builder.build(
                List.of(document(1L, "자혜학교", "주소: 경기도 수원시")),
                3, true, InfoSubCategory.SPECIAL_SCHOOL);

        assertThat(answer).startsWith(
                "요청하신 3곳 중 이전에 안내한 항목을 제외하고 "
                        + "현재 보듬에서 확인한 특수학교 1곳을 안내드립니다.");
    }

    @Test
    void omitsCountWhenUserDidNotRequestOne() {
        String answer = builder.build(
                List.of(
                        document(1L, "부산동암학교", "지역: 부산광역시\n주소: 부산광역시 연제구"),
                        document(2L, "부산구화학교", "지역: 부산광역시\n주소: 부산광역시 남구")
                ),
                null, false, InfoSubCategory.SPECIAL_SCHOOL);

        assertThat(answer)
                .startsWith("부산광역시 특수학교 목록을 안내드립니다.")
                .doesNotContain("특수학교는 2곳", "확인 가능한");
    }

    @Test
    void omitsCountForAdditionalRequestWithoutExplicitCount() {
        String answer = builder.build(
                List.of(document(
                        1L, "부산배화학교", "지역: 부산광역시\n주소: 부산광역시 수영구")),
                null, true, InfoSubCategory.SPECIAL_SCHOOL);

        assertThat(answer)
                .startsWith("이전에 안내한 항목을 제외하고 "
                        + "부산광역시 특수학교 목록을 추가로 안내드립니다.")
                .doesNotContain("1곳입니다");
    }

    @Test
    void explainsMaximumWhenRequestedCountExceedsLimit() {
        String answer = builder.build(
                List.of(
                        document(1L, "부산동암학교", "지역: 부산광역시"),
                        document(2L, "부산구화학교", "지역: 부산광역시")
                ),
                15, false, InfoSubCategory.SPECIAL_SCHOOL);

        assertThat(answer).startsWith(
                "한 번에 최대 10곳까지 안내할 수 있어, 현재 보듬에서 확인한 "
                        + "부산광역시 특수학교 2곳을 안내드립니다.");
    }

    @Test
    void explainsWhenRegionPriorityResultsAreSupplementedFromOtherRegions() {
        String answer = builder.build(
                List.of(
                        document(1L, "자혜학교", "지역: 경기도 수원시"),
                        document(2L, "성남혜은학교", "지역: 경기도 성남시")
                ),
                2, false, InfoSubCategory.SPECIAL_SCHOOL,
                AiSearchScope.REGION_PRIORITY, "경기도 수원시");

        assertThat(answer).startsWith(
                "요청하신 2곳 중 현재 보듬에서 확인 가능한 수원시 특수학교는 1곳입니다. "
                        + "부족한 1곳은 다른 지역의 특수학교로 보충했습니다.");
    }

    @Test
    void explainsWhenAllResultsComeFromOtherRegions() {
        String answer = builder.build(
                List.of(
                        document(1L, "성남혜은학교", "지역: 경기도 성남시"),
                        document(2L, "용인강남학교", "지역: 경기도 용인시")
                ),
                2, false, InfoSubCategory.SPECIAL_SCHOOL,
                AiSearchScope.REGION_PRIORITY, "경기도 수원시");

        assertThat(answer).startsWith(
                "수원시에서 확인 가능한 특수학교를 찾지 못해, "
                        + "요청하신 2곳은 다른 지역의 특수학교로 안내드립니다.");
    }

    @Test
    void explainsWhenOtherRegionsAlsoHaveFewerResultsThanRequested() {
        String answer = builder.build(
                List.of(document(1L, "성남혜은학교", "지역: 경기도 성남시")),
                3, false, InfoSubCategory.SPECIAL_SCHOOL,
                AiSearchScope.REGION_PRIORITY, "경기도 수원시");

        assertThat(answer).startsWith(
                "수원시에서 확인 가능한 특수학교를 찾지 못해, 요청하신 3곳 중 "
                        + "현재 보듬에서 확인한 다른 지역의 특수학교 1곳을 안내드립니다.");
    }

    private AiReferenceDocument document(Long id, String title, String content) {
        return new AiReferenceDocument(
                "INFO-" + id, content, AiResponseSourceType.INFO,
                id, title, "https://example.com/" + id, null);
    }
}
