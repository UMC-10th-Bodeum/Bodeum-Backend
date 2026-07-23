package com.bodeum.domain.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.repository.AiChatRoomRepository;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.ai.repository.AiResponseSourceRepository;
import com.bodeum.domain.ai.repository.projection.AiResponseSourceProjection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiMessageQueryServiceTest {

    @Mock AiChatRoomRepository aiChatRoomRepository;
    @Mock AiMessageRepository aiMessageRepository;
    @Mock AiResponseSourceRepository aiResponseSourceRepository;

    private AiMessageQueryService service;

    @BeforeEach
    void setUp() {
        service = new AiMessageQueryService(
                aiChatRoomRepository,
                aiMessageRepository,
                aiResponseSourceRepository
        );
    }

    @Test
    void returnsTodayMessagesUsingCommonMessageResponse() {
        AiChatRoom chatRoom = mock(AiChatRoom.class);
        when(chatRoom.getId()).thenReturn(7L);
        when(aiChatRoomRepository.findByUserId(1L)).thenReturn(Optional.of(chatRoom));

        AiMessage userMessage = mock(AiMessage.class);
        when(userMessage.getId()).thenReturn(21L);
        when(userMessage.getSenderType()).thenReturn(SenderType.USER);
        when(userMessage.getContent()).thenReturn("질문");
        when(userMessage.getCreatedAt()).thenReturn(Instant.parse("2026-07-21T01:00:00Z"));

        AiMessage aiMessage = mock(AiMessage.class);
        when(aiMessage.getId()).thenReturn(22L);
        when(aiMessage.getSenderType()).thenReturn(SenderType.AI);
        when(aiMessage.getAiAnswerStatus()).thenReturn(AiAnswerStatus.LINK_GUIDANCE);
        when(aiMessage.getContent()).thenReturn("관련 사이트를 확인해 주세요.");
        when(aiMessage.getCreatedAt()).thenReturn(Instant.parse("2026-07-21T01:00:01Z"));

        when(aiMessageRepository.findTodayMessages(eq(7L), any(), any()))
                .thenReturn(List.of(userMessage, aiMessage));

        AiResponseSourceProjection source = mock(AiResponseSourceProjection.class);
        when(source.getAiMessageId()).thenReturn(22L);
        when(source.getSourceType()).thenReturn(AiResponseSourceType.SITE);
        when(source.getSourceId()).thenReturn(3L);
        when(source.getSourceTitle()).thenReturn("복지 사이트");
        when(source.getSourceUrl()).thenReturn("https://example.com");
        when(aiResponseSourceRepository.findAllByMessageIds(List.of(21L, 22L)))
                .thenReturn(List.of(source));

        var result = service.getTodayMessages(1L);

        assertThat(result.messages()).hasSize(2);
        assertThat(result.messages().getFirst().answerStatus()).isNull();
        assertThat(result.messages().getFirst().sources()).isEmpty();
        assertThat(result.messages().getLast().answerStatus())
                .isEqualTo(AiAnswerStatus.LINK_GUIDANCE);
        assertThat(result.messages().getLast().sources().getFirst().sourceId()).isEqualTo(3L);
    }
}
