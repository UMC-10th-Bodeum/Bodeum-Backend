package com.bodeum.domain.ai.repository;

import static com.bodeum.domain.ai.entity.QAiSourceReview.aiSourceReview;

import com.bodeum.domain.ai.enums.AiSourceReviewStatus;
import com.bodeum.domain.ai.model.rag.AiSourceKey;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.Collection;

public class AiSourceReviewQueryRepositoryImpl implements AiSourceReviewQueryRepository {

    private final JPAQueryFactory queryFactory;

    public AiSourceReviewQueryRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public boolean existsWarningRequiredBySources(Collection<AiSourceKey> sourceKeys) {
        if (sourceKeys.isEmpty()) {
            return false;
        }

        BooleanBuilder sourceCondition = new BooleanBuilder();
        sourceKeys.forEach(sourceKey -> sourceCondition.or(
                aiSourceReview.sourceType.eq(sourceKey.sourceType())
                        .and(aiSourceReview.sourceId.eq(sourceKey.sourceId()))
        ));

        return queryFactory
                .selectOne()
                .from(aiSourceReview)
                .where(
                        aiSourceReview.reviewStatus.in(
                                AiSourceReviewStatus.REVIEW_REQUIRED,
                                AiSourceReviewStatus.CONFIRMED_INCORRECT
                        ),
                        sourceCondition
                )
                .fetchFirst() != null;
    }
}
