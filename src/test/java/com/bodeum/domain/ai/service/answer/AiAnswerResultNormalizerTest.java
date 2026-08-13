package com.bodeum.domain.ai.service.answer;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.ai.model.answer.GeneratedAiAnswer;
import com.bodeum.domain.ai.model.answer.GeneratedAiAnswerItem;
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
}
