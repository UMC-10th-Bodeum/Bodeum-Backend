package com.bodeum.domain.ai.model.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.ai.enums.AiQuestionIntent;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiQuestionAnalysisTest {

    @Test
    void normalizesAndLimitsRetrievalQueries() {
        AiQuestionAnalysis analysis = new AiQuestionAnalysis(
                AiQuestionIntent.NONE,
                List.of(" 원문 ", "공식 제도명", "공식 제도명", "세 번째", "네 번째")
        );

        assertThat(analysis.retrievalQueries())
                .containsExactly("원문", "공식 제도명", "세 번째");
    }

    @Test
    void fallsBackToNoneWithNoExpandedQuery() {
        AiQuestionAnalysis analysis = AiQuestionAnalysis.fallback();

        assertThat(analysis.intent()).isEqualTo(AiQuestionIntent.NONE);
        assertThat(analysis.retrievalQueries()).isEmpty();
    }
}
