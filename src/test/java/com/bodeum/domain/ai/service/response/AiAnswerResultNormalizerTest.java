package com.bodeum.domain.ai.service.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.ai.model.answer.GeneratedAiAnswer;
import com.bodeum.domain.ai.model.answer.GeneratedAiAnswerItem;
import com.bodeum.domain.info.entity.enums.InfoSubCategory;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiAnswerResultNormalizerTest {

    private final AiAnswerResultNormalizer normalizer = new AiAnswerResultNormalizer();

    @Test
    void unifiesAdditionalResultCountMessageAsRelatedItems() {
        GeneratedAiAnswer generated = new GeneratedAiAnswer(
                "학교 두 곳을 안내합니다.\n\n"
                        + "이전에 안내한 항목을 제외하면, 추가로 확인 가능한 관련 학교는 0개입니다.",
                List.of("1", "2"),
                List.of(
                        new GeneratedAiAnswerItem("학교1", "1"),
                        new GeneratedAiAnswerItem("학교2", "2")
                )
        );

        GeneratedAiAnswer normalized =
                normalizer.normalizeListedResultCount(generated, 3, true);

        assertThat(normalized.answer())
                .doesNotContain("관련 학교는 0개입니다.")
                .containsOnlyOnce("추가로 확인 가능한 관련 항목은 2개입니다.");
    }

    @Test
    void explainsWhenBodeumHasFewerRehabCentersThanRequested() {
        GeneratedAiAnswer generated = new GeneratedAiAnswer(
                "수원시에서 확인 가능한 치료·재활기관은 6개입니다.\n\n기관 목록",
                List.of("1", "2", "3", "4", "5", "6"),
                List.of(
                        new GeneratedAiAnswerItem("기관1", "1"),
                        new GeneratedAiAnswerItem("기관2", "2"),
                        new GeneratedAiAnswerItem("기관3", "3"),
                        new GeneratedAiAnswerItem("기관4", "4"),
                        new GeneratedAiAnswerItem("기관5", "5"),
                        new GeneratedAiAnswerItem("기관6", "6")
                )
        );

        GeneratedAiAnswer normalized = normalizer.normalizeListedResultCount(
                generated, 10, false, InfoSubCategory.THERAPY_REHAB);

        assertThat(normalized.answer())
                .containsOnlyOnce(
                        "요청하신 10곳 중 현재 보듬에서 확인 가능한 치료·재활기관은 6곳입니다.")
                .doesNotContain("수원시에서 확인 가능한 치료·재활기관은 6개입니다.");
    }

    @Test
    void preservesContentWhenCountExpressionSharesTheLineWithInstitutionName() {
        GeneratedAiAnswer generated = new GeneratedAiAnswer(
                "**이안아동발달연구소** 확인 가능한 기관은 1곳입니다.",
                List.of("1"),
                List.of(new GeneratedAiAnswerItem("이안아동발달연구소", "1"))
        );

        GeneratedAiAnswer normalized = normalizer.normalizeListedResultCount(
                generated, 3, false, InfoSubCategory.THERAPY_REHAB);

        assertThat(normalized.answer())
                .startsWith("**이안아동발달연구소** 확인 가능한 기관은 1곳입니다.")
                .contains("현재 보듬에서 확인 가능한 치료·재활기관은 1곳입니다.");
    }

    @Test
    void removesOnlyStandaloneRequestedCountMessage() {
        GeneratedAiAnswer generated = new GeneratedAiAnswer(
                "기관 목록\n\n"
                        + "요청하신 5곳 중 현재 보듬에서 확인 가능한 치료·재활기관은 2곳입니다.",
                List.of("1"),
                List.of(new GeneratedAiAnswerItem("기관1", "1"))
        );

        GeneratedAiAnswer normalized = normalizer.normalizeListedResultCount(
                generated, 3, false, InfoSubCategory.THERAPY_REHAB);

        assertThat(normalized.answer())
                .startsWith("기관 목록")
                .doesNotContain("요청하신 5곳 중")
                .containsOnlyOnce("현재 보듬에서 확인 가능한 치료·재활기관은 1곳입니다.");
    }
}
