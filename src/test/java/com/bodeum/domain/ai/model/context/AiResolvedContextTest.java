package com.bodeum.domain.ai.model.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AiResolvedContextTest {

    @Test
    void normalizesEmptyRegionToNull() {
        AiResolvedContext context = new AiResolvedContext(
                null,
                new AiResolvedContext.RegionContext(" ", null),
                Map.of(),
                null,
                null
        );

        assertThat(context.region()).isNull();
        assertThat(context.isEmpty()).isTrue();
    }

    @Test
    void ignoresEmptyRegionWhenMergingFollowUpContext() {
        AiResolvedContext original = new AiResolvedContext(
                "특수학교",
                new AiResolvedContext.RegionContext("경기도", "수원시"),
                Map.of(),
                "목록",
                5
        );
        AiResolvedContext update = new AiResolvedContext(
                null,
                new AiResolvedContext.RegionContext(null, null),
                Map.of("설립구분", "공립"),
                null,
                null
        );

        AiResolvedContext merged = original.merge(update);

        assertThat(merged.region()).isEqualTo(original.region());
        assertThat(merged.filters()).containsEntry("설립구분", "공립");
    }

    @Test
    void preservesTopicAcrossRegionAndFilterFollowUps() {
        AiResolvedContext original = new AiResolvedContext(
                "특수학교",
                new AiResolvedContext.RegionContext("경기도", "성남시"),
                Map.of(),
                "목록",
                6
        );

        AiResolvedContext changedRegion = original.withRegion("경기도", "안양시");
        AiResolvedContext publicOnly = changedRegion.merge(new AiResolvedContext(
                null,
                null,
                Map.of("설립구분", "공립"),
                null,
                null
        ));

        assertThat(changedRegion.toResolvedQuestion("안양시는?"))
                .isEqualTo("경기도 안양시 특수학교 알려줘");
        assertThat(publicOnly.toResolvedQuestion("그 중에서 공립은?"))
                .isEqualTo("경기도 안양시 공립 특수학교 알려줘");
    }

    @Test
    void changesRequestedInformationWithoutLosingExistingContext() {
        AiResolvedContext original = new AiResolvedContext(
                "장애인활동지원",
                new AiResolvedContext.RegionContext("경기도", "수원시"),
                Map.of("대상", "장애아동"),
                "목록",
                null
        );

        AiResolvedContext application = original.merge(new AiResolvedContext(
                null, null, Map.of(), "신청 방법", null));

        assertThat(application.toResolvedQuestion("신청 방법은?"))
                .isEqualTo("경기도 수원시 장애아동 장애인활동지원 신청 방법 알려줘");
    }

    @Test
    void inheritsRequestedCountUntilFollowUpChangesIt() {
        AiResolvedContext original = new AiResolvedContext(
                "특수학교", null, Map.of(), "목록", 6);

        AiResolvedContext inherited = original.merge(new AiResolvedContext(
                null, null, Map.of(), null, null));
        AiResolvedContext changed = inherited.merge(new AiResolvedContext(
                null, null, Map.of(), null, 3));

        assertThat(inherited.requestedResultCount()).isEqualTo(6);
        assertThat(changed.requestedResultCount()).isEqualTo(3);
    }
}
