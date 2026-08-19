package com.bodeum.domain.ai.service.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.bodeum.domain.ai.model.context.AiAdditionalResultsContext;
import com.bodeum.domain.ai.model.context.AiSearchQueryContext;
import com.bodeum.domain.ai.model.question.AiSearchScope;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiQuestionSearchQueryBuilderTest {

    private final AiQuestionSearchQueryBuilder builder =
            new AiQuestionSearchQueryBuilder();

    @Test
    void limitsExpandedQueriesUsingConfiguredCount() {
        builder.configureMaxQueryCount(2);

        AiSearchQueryContext context = builder.build(
                "원본 질문",
                List.of("확장 질문 1", "확장 질문 2", "확장 질문 3"),
                null,
                AiSearchScope.REGION_PRIORITY,
                null,
                AiAdditionalResultsContext.empty());

        assertThat(context.queries()).containsExactly("확장 질문 1", "확장 질문 2");
    }

    @Test
    void rejectsOutOfRangeQueryCount() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> builder.configureMaxQueryCount(0));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> builder.configureMaxQueryCount(4));
    }
}
