package com.bodeum.domain.ai.repository;

import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.AiResponseProcessingStatus;
import com.bodeum.domain.ai.enums.SenderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    long countByChatRoomIdAndSenderTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long chatRoomId,
            SenderType senderType,
            Instant startAt,
            Instant endAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AiMessage message
               set message.aiResponseStatus = :failedStatus
             where message.senderType = :senderType
               and message.aiResponseStatus = :processingStatus
               and message.createdAt < :cutoff
            """)
    int markStaleProcessingMessages(
            @Param("senderType") SenderType senderType,
            @Param("processingStatus") AiResponseProcessingStatus processingStatus,
            @Param("failedStatus") AiResponseProcessingStatus failedStatus,
            @Param("cutoff") Instant cutoff
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
