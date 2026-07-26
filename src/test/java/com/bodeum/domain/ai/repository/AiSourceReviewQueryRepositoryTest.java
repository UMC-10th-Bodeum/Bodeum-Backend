package com.bodeum.domain.ai.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.ai.entity.AiSourceReview;
import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.enums.AiSourceReviewStatus;
import com.bodeum.domain.ai.model.rag.AiSourceKey;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "bodeum.auth.jwt-secret=test-jwt-secret-32-bytes-minimum-value")
@Transactional
class AiSourceReviewQueryRepositoryTest {

    @Autowired
    private AiSourceReviewRepository repository;

    @Test
    void matchesSourceTypeAndSourceIdAsOneKey() {
        repository.save(AiSourceReview.create(
                AiResponseSourceType.INFO,
                1L,
                AiSourceReviewStatus.CONFIRMED_INCORRECT,
                "정보 출처 오류"
        ));

        assertThat(repository.existsWarningRequiredBySources(Set.of(
                new AiSourceKey(AiResponseSourceType.INFO, 1L)
        ))).isTrue();
        assertThat(repository.existsWarningRequiredBySources(Set.of(
                new AiSourceKey(AiResponseSourceType.NEWS, 1L),
                new AiSourceKey(AiResponseSourceType.SITE, 1L)
        ))).isFalse();
    }

    @Test
    void returnsTrueWhenAnyExactSourceIsConfirmedIncorrect() {
        repository.save(AiSourceReview.create(
                AiResponseSourceType.SITE,
                7L,
                AiSourceReviewStatus.CONFIRMED_INCORRECT,
                "사이트 출처 오류"
        ));

        assertThat(repository.existsWarningRequiredBySources(Set.of(
                new AiSourceKey(AiResponseSourceType.INFO, 3L),
                new AiSourceKey(AiResponseSourceType.SITE, 7L)
        ))).isTrue();
    }

    @Test
    void returnsTrueWhenSourceReviewIsRequired() {
        repository.save(AiSourceReview.create(
                AiResponseSourceType.NEWS,
                11L,
                AiSourceReviewStatus.REVIEW_REQUIRED,
                null
        ));

        assertThat(repository.existsWarningRequiredBySources(Set.of(
                new AiSourceKey(AiResponseSourceType.NEWS, 11L)
        ))).isTrue();
    }
}
