package com.bodeum.domain.ai.infrastructure.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.ai.model.question.AiResultType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.converter.BeanOutputConverter;

class SpringAiQuestionIntentClassifierTest {

    @Test
    void generatesFilterArrayInsteadOfOpenEndedMapSchema() throws Exception {
        String schema = new BeanOutputConverter<>(
                SpringAiQuestionIntentClassifier.ClassificationResult.class)
                .getJsonSchema();
        var resolvedContextSchema = new ObjectMapper().readTree(schema)
                .path("properties")
                .path("resolvedContext");

        assertThat(resolvedContextSchema.path("additionalProperties").asBoolean()).isFalse();
        assertThat(resolvedContextSchema.path("properties")
                .path("filters").path("type").asText()).isEqualTo("array");
    }

    @Test
    void convertsStructuredFilterArrayToDomainFilterMap() {
        var context = new SpringAiQuestionIntentClassifier.ClassificationResolvedContext(
                "특수학교",
                new SpringAiQuestionIntentClassifier.ClassificationRegion(
                        "부산광역시", null),
                List.of(
                        new SpringAiQuestionIntentClassifier.ClassificationFilter(
                                "설립구분", "공립"),
                        new SpringAiQuestionIntentClassifier.ClassificationFilter(
                                " 장애영역 ", " 지적장애 "),
                        new SpringAiQuestionIntentClassifier.ClassificationFilter(
                                "", "제외"),
                        new SpringAiQuestionIntentClassifier.ClassificationFilter(
                                "제외", "   ")
                ),
                "목록",
                7,
                AiResultType.RESOURCE_LIST
        ).toDomain();

        assertThat(context.topic()).isEqualTo("특수학교");
        assertThat(context.region().displayName()).isEqualTo("부산광역시");
        assertThat(context.filters())
                .containsEntry("설립구분", "공립")
                .containsEntry("장애영역", "지적장애");
        assertThat(context.filters()).doesNotContainKeys("", "제외");
        assertThat(context.requestedResultCount()).isEqualTo(7);
        assertThat(context.resultType()).isEqualTo(AiResultType.RESOURCE_LIST);
    }

    @Test
    void excludesConflictingDuplicateFilterNames() {
        var context = new SpringAiQuestionIntentClassifier.ClassificationResolvedContext(
                "특수학교",
                null,
                List.of(
                        new SpringAiQuestionIntentClassifier.ClassificationFilter(
                                "설립구분", "공립"),
                        new SpringAiQuestionIntentClassifier.ClassificationFilter(
                                "설립구분", "사립"),
                        new SpringAiQuestionIntentClassifier.ClassificationFilter(
                                "장애영역", "지적장애"),
                        new SpringAiQuestionIntentClassifier.ClassificationFilter(
                                "장애영역", "지적장애")
                ),
                "목록",
                null,
                AiResultType.RESOURCE_LIST
        ).toDomain();

        assertThat(context.filters())
                .doesNotContainKey("설립구분")
                .containsEntry("장애영역", "지적장애");
    }
}
