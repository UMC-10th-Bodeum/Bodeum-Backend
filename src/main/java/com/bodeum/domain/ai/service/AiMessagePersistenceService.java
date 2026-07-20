package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.entity.AiResponseSource;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.repository.AiChatRoomRepository;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.ai.repository.AiResponseSourceRepository;
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
    public AiMessage saveUserMessage(AiChatRoom chatRoom, String content) {
        AiMessage message = aiMessageRepository.save(AiMessage.createUserMessage(chatRoom, content));
        chatRoom.updateLastMessageAt(Instant.now());
        aiChatRoomRepository.save(chatRoom);
        return message;
    }

    @Transactional
    public AiMessage saveAiMessage(
            AiChatRoom chatRoom,
            String content,
            boolean warning,
            List<AiReferenceDocument> sources
    ) {
        AiMessage message = aiMessageRepository.save(AiMessage.createAiMessage(chatRoom, content, warning));
        aiResponseSourceRepository.saveAll(sources.stream()
                .map(source -> AiResponseSource.create(
                        message, source.sourceType(), source.sourceId(), source.title(),
                        source.url(), source.updatedAt()))
                .toList());
        chatRoom.updateLastMessageAt(Instant.now());
        aiChatRoomRepository.save(chatRoom);
        return message;
    }
}
