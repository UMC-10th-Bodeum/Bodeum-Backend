package com.bodeum.domain.ai.repository;

import com.bodeum.domain.ai.entity.AiFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {

    @Modifying
    @Query("delete from AiFeedback f where f.aiMessage.id in (select m.id from AiMessage m where m.chatRoom.id = :chatRoomId)")
    int deleteByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}
