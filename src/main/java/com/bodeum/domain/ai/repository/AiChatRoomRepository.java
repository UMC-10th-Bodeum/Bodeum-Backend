package com.bodeum.domain.ai.repository;

import com.bodeum.domain.ai.entity.AiChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiChatRoomRepository extends JpaRepository<AiChatRoom, Long> {

    Optional<AiChatRoom> findByUserId(Long userId);
}
