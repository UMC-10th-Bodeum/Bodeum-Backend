package com.bodeum.domain.ai.service.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.ai.model.answer.GeneratedAiAnswer;
import com.bodeum.domain.ai.model.answer.GeneratedAiAnswerItem;
import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
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
                        "요청하신 10곳 중 현재 보듬에서 확인 가능한 치료·재활기관 6곳을 안내드립니다.")
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
                .contains("현재 보듬에서 확인 가능한 치료·재활기관 1곳을 안내드립니다.");
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
                .containsOnlyOnce("현재 보듬에서 확인 가능한 치료·재활기관 1곳을 안내드립니다.");
    }

    @Test
    void removesDuplicateCountMessagesFromSameLineAndIncludesRegion() {
        GeneratedAiAnswer generated = new GeneratedAiAnswer(
                "수원시 특수학교로 안내할 수 있는 곳은 3개입니다. "
                        + "현재 확인 가능한 관련 항목은 3개입니다.\n\n학교 목록",
                List.of("1", "2", "3"),
                List.of(
                        new GeneratedAiAnswerItem("학교1", "1"),
                        new GeneratedAiAnswerItem("학교2", "2"),
                        new GeneratedAiAnswerItem("학교3", "3")
                )
        );

        GeneratedAiAnswer normalized = normalizer.normalizeListedResultCount(
                generated, 7, false, InfoSubCategory.SPECIAL_SCHOOL, "수원시");

        assertThat(normalized.answer())
                .startsWith("학교 목록")
                .doesNotContain("안내할 수 있는 곳", "관련 항목은")
                .containsOnlyOnce(
                        "요청하신 7곳 중 현재 보듬에서 확인 가능한 수원시 특수학교 3곳을 안내드립니다.");
    }

    @Test
    void usesConsistentMessageWhenRequestedCountIsSatisfied() {
        GeneratedAiAnswer generated = new GeneratedAiAnswer(
                "부산광역시에서 확인 가능한 특수학교는 5개입니다.\n\n학교 목록",
                List.of("1", "2", "3", "4", "5"),
                List.of(
                        new GeneratedAiAnswerItem("학교1", "1"),
                        new GeneratedAiAnswerItem("학교2", "2"),
                        new GeneratedAiAnswerItem("학교3", "3"),
                        new GeneratedAiAnswerItem("학교4", "4"),
                        new GeneratedAiAnswerItem("학교5", "5")
                )
        );

        GeneratedAiAnswer normalized = normalizer.normalizeListedResultCount(
                generated, 5, false, InfoSubCategory.SPECIAL_SCHOOL, "부산광역시");

        assertThat(normalized.answer())
                .startsWith("학교 목록")
                .containsOnlyOnce(
                        "요청하신 개수에 맞춰 현재 보듬에서 확인 가능한 부산광역시 특수학교 5곳을 안내드립니다.");
    }

    @Test
    void removesCountMessageWhenCountWasNotRequested() {
        GeneratedAiAnswer generated = new GeneratedAiAnswer(
                "부산광역시에서 확인 가능한 특수학교는 5개입니다.\n\n학교 목록",
                List.of("1"),
                List.of(new GeneratedAiAnswerItem("학교1", "1"))
        );

        GeneratedAiAnswer normalized = normalizer.normalizeListedResultCount(
                generated, null, false, InfoSubCategory.SPECIAL_SCHOOL, "부산광역시");

        assertThat(normalized.answer())
                .isEqualTo("학교 목록")
                .doesNotContain("5개", "5곳");
    }

    @Test
    void removesStaleCountMessageWhenNoAnswerItemsRemain() {
        GeneratedAiAnswer generated = new GeneratedAiAnswer(
                "현재 확인 가능한 관련 기관은 3개입니다.\n\n관련 정보를 찾지 못했습니다.",
                List.of(),
                List.of()
        );

        GeneratedAiAnswer normalized = normalizer.normalizeListedResultCount(
                generated, 5, false, InfoSubCategory.THERAPY_REHAB, "경기도 수원시");

        assertThat(normalized.answer())
                .isEqualTo("관련 정보를 찾지 못했습니다.")
                .doesNotContain("3개");
    }

    @Test
    void normalizesExternalSiteListUsingTheSamePolicy() {
        String normalized = normalizer.normalizeExternalListAnswer(
                "현재 확인 가능한 공식 사이트는 3곳입니다.\n\n사이트 목록",
                3, 5, false, null, null, true, true);

        assertThat(normalized)
                .startsWith("요청하신 5곳 중 현재 보듬에서 확인 가능한 "
                        + "공식 사이트 3곳을 안내드립니다.\n\n사이트 목록")
                .containsOnlyOnce(
                        "요청하신 5곳 중 현재 보듬에서 확인 가능한 공식 사이트 3곳을 안내드립니다.");
    }

    @Test
    void explainsMaximumWhenRequestedCountExceedsLimit() {
        GeneratedAiAnswer generated = new GeneratedAiAnswer(
                "현재 확인 가능한 관련 항목은 10개입니다.\n\n학교 목록",
                java.util.stream.LongStream.rangeClosed(1, 10)
                        .mapToObj(Long::toString).toList(),
                java.util.stream.LongStream.rangeClosed(1, 10)
                        .mapToObj(index -> new GeneratedAiAnswerItem(
                                "학교" + index, Long.toString(index)))
                        .toList()
        );

        GeneratedAiAnswer normalized = normalizer.normalizeListedResultCount(
                generated, 15, false, InfoSubCategory.SPECIAL_SCHOOL, "부산광역시");

        assertThat(normalized.answer()).containsOnlyOnce(
                "한 번에 최대 10곳까지 안내할 수 있어, 현재 보듬에서 확인 가능한 "
                        + "부산광역시 특수학교 10곳을 안내드립니다.");
    }

    @Test
    void explainsMixedRegionCompositionForRagListAnswer() {
        GeneratedAiAnswer generated = new GeneratedAiAnswer(
                "요청하신 대로 특수학교 2개를 안내드립니다.\n\n학교 목록",
                List.of("LOCAL", "OTHER"),
                List.of(
                        new GeneratedAiAnswerItem("자혜학교", "LOCAL"),
                        new GeneratedAiAnswerItem("성남혜은학교", "OTHER")
                )
        );
        List<AiReferenceDocument> documents = List.of(
                new AiReferenceDocument(
                        "LOCAL", "지역: 경기도 수원시", AiResponseSourceType.INFO,
                        1L, "자혜학교", null, null),
                new AiReferenceDocument(
                        "OTHER", "지역: 경기도 성남시", AiResponseSourceType.INFO,
                        2L, "성남혜은학교", null, null)
        );

        GeneratedAiAnswer normalized = normalizer.normalizeListedResultCount(
                generated, 2, false, InfoSubCategory.SPECIAL_SCHOOL,
                "경기도 수원시", true, documents, AiSearchScope.REGION_PRIORITY);

        assertThat(normalized.answer()).containsOnlyOnce(
                "요청하신 2곳 중 현재 보듬에서 확인 가능한 수원시 특수학교는 1곳입니다. "
                        + "부족한 1곳은 다른 지역의 특수학교로 보충했습니다.");
    }

    @Test
    void explainsWhenAllRagResultsComeFromOtherRegions() {
        GeneratedAiAnswer generated = new GeneratedAiAnswer(
                "특수학교 목록",
                List.of("OTHER-1", "OTHER-2"),
                List.of(
                        new GeneratedAiAnswerItem("성남혜은학교", "OTHER-1"),
                        new GeneratedAiAnswerItem("용인강남학교", "OTHER-2")
                )
        );
        List<AiReferenceDocument> documents = List.of(
                new AiReferenceDocument(
                        "OTHER-1", "지역: 경기도 성남시", AiResponseSourceType.INFO,
                        1L, "성남혜은학교", null, null),
                new AiReferenceDocument(
                        "OTHER-2", "지역: 경기도 용인시", AiResponseSourceType.INFO,
                        2L, "용인강남학교", null, null)
        );

        GeneratedAiAnswer normalized = normalizer.normalizeListedResultCount(
                generated, 2, false, InfoSubCategory.SPECIAL_SCHOOL,
                "경기도 수원시", true, documents, AiSearchScope.REGION_PRIORITY);

        assertThat(normalized.answer()).containsOnlyOnce(
                "수원시에서 확인 가능한 특수학교를 찾지 못해, "
                + "요청하신 2곳은 다른 지역의 특수학교로 안내드립니다.");
    }

    @Test
    void usesCorrectKoreanParticlesForWelfareCenterLabels() {
        GeneratedAiAnswer generated = new GeneratedAiAnswer(
                "복지관 목록",
                List.of("OTHER-1", "OTHER-2"),
                List.of(
                        new GeneratedAiAnswerItem("성남시복지관", "OTHER-1"),
                        new GeneratedAiAnswerItem("용인시복지관", "OTHER-2")
                )
        );
        List<AiReferenceDocument> documents = List.of(
                new AiReferenceDocument(
                        "OTHER-1", "지역: 경기도 성남시", AiResponseSourceType.INFO,
                        1L, "성남시복지관", null, null),
                new AiReferenceDocument(
                        "OTHER-2", "지역: 경기도 용인시", AiResponseSourceType.INFO,
                        2L, "용인시복지관", null, null)
        );

        GeneratedAiAnswer normalized = normalizer.normalizeListedResultCount(
                generated, 2, false, InfoSubCategory.WELFARE_CENTER,
                "경기도 수원시", true, documents, AiSearchScope.REGION_PRIORITY);

        assertThat(normalized.answer())
                .contains("장애인복지관을 찾지 못해")
                .contains("장애인복지관으로 안내드립니다")
                .doesNotContain("장애인복지관를", "장애인복지관로");
    }
}
