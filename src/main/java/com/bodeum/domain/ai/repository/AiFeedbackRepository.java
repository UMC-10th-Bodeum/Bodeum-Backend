package com.bodeum.domain.ai.repository;

import com.bodeum.domain.ai.entity.AiFeedback;
import com.bodeum.domain.ai.repository.projection.AiFeedbackProjection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {

    boolean existsByAiMessageId(Long aiMessageId);

    @Query("""
            select feedback.aiMessage.id as aiMessageId,
                   feedback.id as aiFeedbackId,
                   feedback.feedbackType as feedbackType,
                   feedbackReason.reason as reason
              from AiFeedback feedback
              left join AiFeedbackReason feedbackReason
                on feedbackReason.aiFeedback = feedback
             where feedback.aiMessage.id in :messageIds
             order by feedback.id asc, feedbackReason.id asc
            """)
    List<AiFeedbackProjection> findAllWithReasonsByMessageIds(
            @Param("messageIds") List<Long> messageIds
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from AiFeedback feedback
             where feedback.aiMessage.chatRoom.user.id = :userId
            """)
    int deleteByUserId(@Param("userId") Long userId);
}
