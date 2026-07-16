package com.bodeum.domain.ai.repository;

import com.bodeum.domain.ai.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

    boolean existsByChatRoomIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long chatRoomId,
            Instant startAt,
            Instant endAt
    );

    @Query("""
        SELECT m
        FROM AiMessage m
        WHERE m.chatRoom.id = :chatRoomId
          AND m.createdAt >= :startAt
          AND m.createdAt < :endAt
        ORDER BY m.createdAt ASC
        """)
    List<AiMessage> findTodayMessages(
            @Param("chatRoomId") Long chatRoomId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt
    );
}
