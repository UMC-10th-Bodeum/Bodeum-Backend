package com.bodeum.domain.ai.repository;

import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.SenderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

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
}
