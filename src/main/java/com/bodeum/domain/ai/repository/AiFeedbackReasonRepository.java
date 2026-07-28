package com.bodeum.domain.ai.repository;

import com.bodeum.domain.ai.entity.AiFeedbackReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiFeedbackReasonRepository extends JpaRepository<AiFeedbackReason, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from AiFeedbackReason reason
             where reason.aiFeedback.id in (
                   select feedback.id
                     from AiFeedback feedback
                    where feedback.aiMessage.chatRoom.user.id = :userId
             )
            """)
    int deleteByUserId(@Param("userId") Long userId);
}
