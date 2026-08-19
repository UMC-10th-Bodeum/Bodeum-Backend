package com.bodeum.domain.ai.service.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.repository.AiChatRoomRepository;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.ai.repository.AiResponseSourceRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiMessagePersistenceServiceTest {

    private final AiMessageRepository messageRepository = mock(AiMessageRepository.class);
    private final AiResponseSourceRepository sourceRepository =
            mock(AiResponseSourceRepository.class);
    private final AiChatRoomRepository chatRoomRepository = mock(AiChatRoomRepository.class);
    private final AiMessagePersistenceService service = new AiMessagePersistenceService(
            messageRepository, sourceRepository, chatRoomRepository);

    @Test
    void savesOnlyOneSourceForTheSameNormalizedUrl() {
        AiChatRoom chatRoom = mock(AiChatRoom.class);
        AiMessage userMessage = AiMessage.createUserMessage(chatRoom, "질문");
        when(messageRepository.findByIdForAiResponse(1L))
                .thenReturn(Optional.of(userMessage));
        when(messageRepository.save(any(AiMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        List<AiReferenceDocument> sources = List.of(
                source("A", 1L, "https://www.heart4u.or.kr/"),
                source("B", 2L, "https://heart4u.or.kr"),
                source("C", 3L, "https://heart4u.or.kr/program/motor")
        );

        service.saveAiMessageAndComplete(
                1L, chatRoom, "답변", false, AiAnswerStatus.ANSWERED, sources);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<com.bodeum.domain.ai.entity.AiResponseSource>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(sourceRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    void rejectsLateAnswerAfterTimedOutMessageWasMarkedFailed() {
        AiChatRoom chatRoom = mock(AiChatRoom.class);
        AiMessage userMessage = AiMessage.createUserMessage(chatRoom, "질문");
        userMessage.failAiResponse();
        when(messageRepository.findByIdForAiResponse(1L))
                .thenReturn(Optional.of(userMessage));

        assertThatThrownBy(() -> service.saveAiMessageAndComplete(
                1L, chatRoom, "늦게 생성된 답변", false,
                AiAnswerStatus.ANSWERED, List.of()))
                .isInstanceOf(ProjectException.class)
                .extracting(error -> ((ProjectException) error).getErrorCode())
                .isEqualTo(AiErrorCode.AI_RESPONSE_TIMEOUT);

        verify(messageRepository, never()).save(
                org.mockito.ArgumentMatchers.any(AiMessage.class));
        verify(sourceRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    private AiReferenceDocument source(String key, Long id, String url) {
        return new AiReferenceDocument(
                key, key, AiResponseSourceType.SITE, id, key, url, null);
    }
}
