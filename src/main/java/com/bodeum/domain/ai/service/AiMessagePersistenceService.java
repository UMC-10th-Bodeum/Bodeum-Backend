package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.entity.AiResponseSource;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.repository.AiChatRoomRepository;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.ai.repository.AiResponseSourceRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiMessagePersistenceService {

    private final AiMessageRepository aiMessageRepository;
    private final AiResponseSourceRepository aiResponseSourceRepository;
    private final AiChatRoomRepository aiChatRoomRepository;

    @Transactional
    public AiMessage saveProcessingUserMessage(AiChatRoom chatRoom, String content) {
        AiMessage message = aiMessageRepository.save(AiMessage.createUserMessage(chatRoom, content));
        chatRoom.updateLastMessageAt(Instant.now());
        aiChatRoomRepository.save(chatRoom);
        return message;
    }

    @Transactional
    public AiMessage saveAiMessageAndComplete(
            Long userMessageId,
            AiChatRoom chatRoom,
            String content,
            boolean warning,
            AiAnswerStatus answerStatus,
            List<AiReferenceDocument> sources
    ) {
        AiMessage userMessage = aiMessageRepository.findById(userMessageId)
                .orElseThrow(() -> new ProjectException(AiErrorCode.AI_RESPONSE_FAILED));
        AiMessage message = aiMessageRepository.save(
                AiMessage.createAiMessage(chatRoom, content, warning, answerStatus));
        aiResponseSourceRepository.saveAll(sources.stream()
                .map(source -> AiResponseSource.create(
                        message, source.sourceType(), source.sourceId(), source.title(),
                        source.url(), source.updatedAt()))
                .toList());
        userMessage.completeAiResponse();
        chatRoom.updateLastMessageAt(Instant.now());
        aiChatRoomRepository.save(chatRoom);
        return message;
    }
}
