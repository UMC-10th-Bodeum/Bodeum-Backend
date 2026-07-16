package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.dto.response.AiGuideConfirmationResponse;
import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.repository.AiChatRoomRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiChatRoomService {

    private final AiChatRoomRepository aiChatRoomRepository;

    @Transactional
    public AiGuideConfirmationResponse confirmGuide(Long userId) {
        AiChatRoom chatRoom = aiChatRoomRepository.findByUserId(userId)
                .orElseThrow(() -> new ProjectException(AiErrorCode.AI_CHAT_ROOM_NOT_FOUND));

        chatRoom.confirmGuide();

        return AiGuideConfirmationResponse.of(
                chatRoom.getLastGuideConfirmedAt()
        );
    }
}
