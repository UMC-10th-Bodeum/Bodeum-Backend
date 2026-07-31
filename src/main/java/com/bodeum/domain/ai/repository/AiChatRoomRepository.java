package com.bodeum.domain.ai.repository;

import com.bodeum.domain.ai.entity.AiChatRoom;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AiChatRoomRepository extends JpaRepository<AiChatRoom, Long> {

    Optional<AiChatRoom> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select chatRoom from AiChatRoom chatRoom where chatRoom.user.id = :userId")
    Optional<AiChatRoom> findByUserIdForUpdate(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from AiChatRoom chatRoom where chatRoom.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
