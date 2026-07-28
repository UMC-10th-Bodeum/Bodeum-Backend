package com.bodeum.domain.ai.repository;

import com.bodeum.domain.ai.entity.AiFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {

    boolean existsByAiMessageId(Long aiMessageId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from AiFeedback feedback
             where feedback.aiMessage.chatRoom.user.id = :userId
            """)
    int deleteByUserId(@Param("userId") Long userId);
}
