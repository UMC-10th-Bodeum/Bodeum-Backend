package com.bodeum.domain.ai.repository;

import com.bodeum.domain.ai.entity.AiSourceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiSourceReviewRepository extends
        JpaRepository<AiSourceReview, Long>,
        AiSourceReviewQueryRepository {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO ai_source_review (
                source_type,
                source_id,
                review_status,
                review_note,
                reviewed_at,
                created_at,
                updated_at
            )
            SELECT
                target.source_type,
                target.source_id,
                'REVIEW_REQUIRED',
                NULL,
                NULL,
                NOW(6),
                NOW(6)
            FROM ai_response_source target
            JOIN ai_message target_message
              ON target_message.ai_message_id = target.ai_message_id
             AND target_message.ai_answer_status = 'ANSWERED'
            JOIN ai_response_source cited
              ON cited.source_type = target.source_type
             AND cited.source_id = target.source_id
            JOIN ai_message cited_message
              ON cited_message.ai_message_id = cited.ai_message_id
             AND cited_message.ai_answer_status = 'ANSWERED'
            JOIN ai_feedback feedback
              ON feedback.ai_message_id = cited.ai_message_id
             AND feedback.feedback_type = 'INCORRECT'
            WHERE target.ai_message_id = :aiMessageId
            GROUP BY target.source_type, target.source_id
            HAVING COUNT(DISTINCT feedback.ai_feedback_id) >= :threshold
            ORDER BY target.source_type, target.source_id
            ON DUPLICATE KEY UPDATE
                review_status = IF(
                    review_status = 'CONFIRMED_INCORRECT',
                    review_status,
                    'REVIEW_REQUIRED'
                ),
                updated_at = IF(
                    review_status = 'CONFIRMED_INCORRECT',
                    updated_at,
                    NOW(6)
                )
            """, nativeQuery = true)
    int markReviewRequiredByIncorrectFeedbackCount(
            @Param("aiMessageId") Long aiMessageId,
            @Param("threshold") int threshold
    );
}
