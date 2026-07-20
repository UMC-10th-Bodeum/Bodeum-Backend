package com.bodeum.domain.ai.repository;

import com.bodeum.domain.ai.entity.AiSourceReview;
import com.bodeum.domain.ai.enums.AiResponseSourceType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSourceReviewRepository extends
        JpaRepository<AiSourceReview, Long>,
        AiSourceReviewQueryRepository {
}
