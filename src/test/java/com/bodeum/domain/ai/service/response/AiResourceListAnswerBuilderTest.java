package com.bodeum.domain.ai.service.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
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
                .startsWith("요청하신 5곳 중 현재 보듬에서 확인 가능한 "
                        + "치료·재활기관은 2곳입니다.")
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
                        + "현재 보듬에서 추가로 확인 가능한 특수학교는 1곳입니다.");
    }

    private AiReferenceDocument document(Long id, String title, String content) {
        return new AiReferenceDocument(
                "INFO-" + id, content, AiResponseSourceType.INFO,
                id, title, "https://example.com/" + id, null);
    }
}
