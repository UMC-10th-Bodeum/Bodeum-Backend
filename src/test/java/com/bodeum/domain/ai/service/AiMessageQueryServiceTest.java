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
import org.springframework.data.domain.Pageable;

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

    @Test
    void returnsPreviousMessagesGroupedByDateWithCursor() {
        AiChatRoom chatRoom = mock(AiChatRoom.class);
        when(chatRoom.getId()).thenReturn(7L);
        when(aiChatRoomRepository.findByUserId(1L)).thenReturn(Optional.of(chatRoom));

        AiMessage latestAi = mock(AiMessage.class);
        when(latestAi.getId()).thenReturn(31L);
        when(latestAi.getSenderType()).thenReturn(SenderType.AI);
        when(latestAi.getAiAnswerStatus()).thenReturn(AiAnswerStatus.ANSWERED);
        when(latestAi.getContent()).thenReturn("07-06 AI");
        when(latestAi.getCreatedAt()).thenReturn(Instant.parse("2026-07-06T07:10:05Z"));
        when(latestAi.isWarning()).thenReturn(false);

        AiMessage latestUser = mock(AiMessage.class);
        when(latestUser.getId()).thenReturn(30L);
        when(latestUser.getSenderType()).thenReturn(SenderType.USER);
        when(latestUser.getContent()).thenReturn("07-06 USER");
        when(latestUser.getCreatedAt()).thenReturn(Instant.parse("2026-07-06T07:10:00Z"));

        AiMessage olderAi = mock(AiMessage.class);
        when(olderAi.getId()).thenReturn(27L);
        when(olderAi.getSenderType()).thenReturn(SenderType.AI);
        when(olderAi.getAiAnswerStatus()).thenReturn(AiAnswerStatus.LINK_GUIDANCE);
        when(olderAi.getContent()).thenReturn("07-05 AI");
        when(olderAi.getCreatedAt()).thenReturn(Instant.parse("2026-07-05T05:20:05Z"));
        when(olderAi.isWarning()).thenReturn(false);

        AiMessage olderUser = mock(AiMessage.class);
        when(olderUser.getId()).thenReturn(26L);
        when(olderUser.getSenderType()).thenReturn(SenderType.USER);
        when(olderUser.getContent()).thenReturn("07-05 USER");
        when(olderUser.getCreatedAt()).thenReturn(Instant.parse("2026-07-05T05:20:00Z"));

        when(aiMessageRepository.findHistoryMessages(eq(7L), any(), any(), eq(null), any(Pageable.class)))
                .thenReturn(List.of(latestAi, latestUser, olderAi, olderUser));

        AiResponseSourceProjection latestSource = mock(AiResponseSourceProjection.class);
        when(latestSource.getAiMessageId()).thenReturn(31L);
        when(latestSource.getSourceType()).thenReturn(AiResponseSourceType.INFO);
        when(latestSource.getSourceId()).thenReturn(101L);
        when(latestSource.getSourceTitle()).thenReturn("07-06 source");
        when(latestSource.getSourceUrl()).thenReturn("https://example.com/info");

        AiResponseSourceProjection olderSource = mock(AiResponseSourceProjection.class);
        when(olderSource.getAiMessageId()).thenReturn(27L);
        when(olderSource.getSourceType()).thenReturn(AiResponseSourceType.SITE);
        when(olderSource.getSourceId()).thenReturn(6L);
        when(olderSource.getSourceTitle()).thenReturn("07-05 source");
        when(olderSource.getSourceUrl()).thenReturn("https://example.com/site");

        when(aiResponseSourceRepository.findAllByMessageIds(List.of(31L, 30L, 27L, 26L)))
                .thenReturn(List.of(latestSource, olderSource));

        var result = service.getHistoryMessages(1L, null);

        assertThat(result.messages()).hasSize(2);
        assertThat(result.messages().getFirst().date().toString()).isEqualTo("2026-07-06");
        assertThat(result.messages().getFirst().items()).hasSize(2);
        assertThat(result.messages().getFirst().items().getFirst().senderType()).isEqualTo(SenderType.USER);
        assertThat(result.messages().getFirst().items().getLast().senderType()).isEqualTo(SenderType.AI);
        assertThat(result.messages().get(1).date().toString()).isEqualTo("2026-07-05");
        assertThat(result.nextCursor()).isEqualTo(26L);
        assertThat(result.hasNext()).isFalse();
    }
}
