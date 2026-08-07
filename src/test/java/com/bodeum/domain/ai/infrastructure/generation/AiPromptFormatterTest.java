package com.bodeum.domain.ai.infrastructure.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.ai.model.rag.AiUserProfile;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiPromptFormatterTest {

    private final AiPromptFormatter formatter = new AiPromptFormatter();

    @Test
    void includesRecentScrapInterestsInPersonalizationContext() {
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시",
                "경기도",
                "수원시",
                7,
                List.of("AUTISM"),
                List.of("EDUCATION"),
                null,
                List.of("수원시 발달재활서비스 제공기관"),
                List.of("2026년 발달재활서비스 신청 안내"),
                List.of("특수학교 정보 (게시판: INFORMATION_QUESTION, 태그: 특수학교)")
        );

        assertThat(formatter.formatProfile(profile)).contains(
                "[최근 스크랩 관심 정보]",
                "정보: [수원시 발달재활서비스 제공기관]",
                "소식: [2026년 발달재활서비스 신청 안내]",
                "커뮤니티 주제: [특수학교 정보"
        );
    }
}
