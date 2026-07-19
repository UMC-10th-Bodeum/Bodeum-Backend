package com.bodeum.domain.ai.repository;

import com.bodeum.domain.ai.entity.AiSourceReview;
import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.enums.AiSourceReviewStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSourceReviewRepository extends JpaRepository<AiSourceReview, Long> {

    Optional<AiSourceReview> findBySourceTypeAndSourceId(
            AiResponseSourceType sourceType,
            Long sourceId
    );

    boolean existsBySourceTypeAndSourceIdAndReviewStatus(
            AiResponseSourceType sourceType,
            Long sourceId,
            AiSourceReviewStatus reviewStatus
    );
}
