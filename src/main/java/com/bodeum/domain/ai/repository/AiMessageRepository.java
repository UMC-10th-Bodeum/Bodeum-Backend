package com.bodeum.domain.ai.repository;

import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.AiResponseProcessingStatus;
import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.repository.projection.AiConversationMessageProjection;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

    @Query("""
            select message.id as id,
                   message.senderType as senderType,
                   message.content as content,
                   message.resolvedQuestion as resolvedQuestion,
                   message.resolvedContext as resolvedContext,
                   message.contextRootMessageId as contextRootMessageId
              from AiMessage message
             where message.chatRoom.id = :chatRoomId
             order by message.createdAt desc, message.id desc
            """)
    List<AiConversationMessageProjection> findRecentConversationContext(
            @Param("chatRoomId") Long chatRoomId,
            Pageable pageable
    );

    Optional<AiMessage> findTopByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
            Long chatRoomId,
            SenderType senderType
    );

    List<AiMessage> findByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
            Long chatRoomId,
            SenderType senderType,
            Pageable pageable
    );

    List<AiMessage> findByChatRoomIdAndContextRootMessageIdAndSenderTypeOrderByCreatedAtDescIdDesc(
            Long chatRoomId,
            Long contextRootMessageId,
            SenderType senderType
    );

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
               set message.aiProcessingStatus = :failedStatus
             where message.senderType = :senderType
               and message.aiProcessingStatus = :processingStatus
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
          AND (
                :cursorCreatedAt IS NULL
                OR m.createdAt < :cursorCreatedAt
                OR (m.createdAt = :cursorCreatedAt AND m.id < :cursorId)
          )
        ORDER BY m.createdAt DESC, m.id DESC
        """)
    List<AiMessage> findTodayMessages(
            @Param("chatRoomId") Long chatRoomId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("cursorId") Long cursorId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            Pageable pageable
    );

    @Query("""
        SELECT m
        FROM AiMessage m
        WHERE m.chatRoom.id = :chatRoomId
          AND m.createdAt >= :startAt
          AND m.createdAt < :endAt
          AND (
                :cursorCreatedAt IS NULL
                OR m.createdAt < :cursorCreatedAt
                OR (m.createdAt = :cursorCreatedAt AND m.id < :cursorId)
          )
        ORDER BY m.createdAt DESC, m.id DESC
        """)
    List<AiMessage> findHistoryMessages(
            @Param("chatRoomId") Long chatRoomId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("cursorId") Long cursorId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("""
        SELECT m
        FROM AiMessage m
        WHERE m.id = :messageId
        """)
    Optional<AiMessage> findByIdForFeedback(
            @Param("messageId") Long messageId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from AiMessage message
             where message.chatRoom.user.id = :userId
            """)
    int deleteByUserId(@Param("userId") Long userId);
}
