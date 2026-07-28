package com.bodeum.domain.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
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
import java.util.ArrayList;
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

        when(aiMessageRepository.findHistoryMessages(eq(7L), any(), any(), eq(null), eq(null)))
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

        var result = service.getHistoryMessages(1L, null, null);

        assertThat(result.messages()).hasSize(2);
        assertThat(result.messages().getFirst().date().toString()).isEqualTo("2026-07-06");
        assertThat(result.messages().getFirst().items()).hasSize(2);
        assertThat(result.messages().getFirst().items().getFirst().senderType()).isEqualTo(SenderType.USER);
        assertThat(result.messages().getFirst().items().getLast().senderType()).isEqualTo(SenderType.AI);
        assertThat(result.messages().get(1).date().toString()).isEqualTo("2026-07-05");
        assertThat(result.nextCursor().id()).isEqualTo(26L);
        assertThat(result.nextCursor().createdAt()).isEqualTo(Instant.parse("2026-07-05T05:20:00Z"));
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void returnsHasNextTrueAndPassesCursorToRepository() {
        AiChatRoom chatRoom = mock(AiChatRoom.class);
        when(chatRoom.getId()).thenReturn(7L);
        when(aiChatRoomRepository.findByUserId(1L)).thenReturn(Optional.of(chatRoom));

        Long cursor = 120L;
        List<AiMessage> fetchedMessages = new ArrayList<>();
        List<Long> expectedMessageIds = new ArrayList<>();

        for (long id = 200L; id >= 180L; id--) {
            AiMessage message = mock(AiMessage.class);
            lenient().when(message.getId()).thenReturn(id);
            lenient().when(message.getSenderType()).thenReturn(SenderType.USER);
            lenient().when(message.getContent()).thenReturn("message-" + id);
            lenient().when(message.getCreatedAt()).thenReturn(id >= 181L
                    ? Instant.parse("2026-07-06T01:00:00Z")
                    : Instant.parse("2026-07-05T01:00:00Z"));
            fetchedMessages.add(message);
            if (id >= 181L) {
                expectedMessageIds.add(id);
            }
        }

        when(aiMessageRepository.findHistoryMessages(eq(7L), any(), any(), eq(cursor), any()))
                .thenReturn(fetchedMessages);
        when(aiResponseSourceRepository.findAllByMessageIds(expectedMessageIds))
                .thenReturn(List.of());

        var result = service.getHistoryMessages(1L, cursor, Instant.parse("2026-07-06T01:00:00Z"));

        verify(aiMessageRepository).findHistoryMessages(eq(7L), any(), any(), eq(cursor), eq(Instant.parse("2026-07-06T01:00:00Z")));
        verify(aiResponseSourceRepository).findAllByMessageIds(expectedMessageIds);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor().id()).isEqualTo(181L);
        assertThat(result.messages()).hasSize(1);
        assertThat(result.messages().getFirst().items()).hasSize(20);
    }

    @Test
    void keepsSameDateMessagesInOnePageEvenWhenLimitIsExceeded() {
        AiChatRoom chatRoom = mock(AiChatRoom.class);
        when(chatRoom.getId()).thenReturn(7L);
        when(aiChatRoomRepository.findByUserId(1L)).thenReturn(Optional.of(chatRoom));

        List<AiMessage> fetchedMessages = new ArrayList<>();
        List<Long> messageIds = new ArrayList<>();

        for (long id = 300L; id >= 280L; id--) {
            AiMessage message = mock(AiMessage.class);
            lenient().when(message.getId()).thenReturn(id);
            lenient().when(message.getSenderType()).thenReturn(SenderType.USER);
            lenient().when(message.getContent()).thenReturn("same-date-" + id);
            lenient().when(message.getCreatedAt()).thenReturn(Instant.parse("2026-07-06T01:00:00Z"));
            fetchedMessages.add(message);
            messageIds.add(id);
        }

        AiMessage olderMessage = mock(AiMessage.class);
        lenient().when(olderMessage.getId()).thenReturn(279L);
        lenient().when(olderMessage.getSenderType()).thenReturn(SenderType.USER);
        lenient().when(olderMessage.getContent()).thenReturn("older");
        lenient().when(olderMessage.getCreatedAt()).thenReturn(Instant.parse("2026-07-05T01:00:00Z"));
        fetchedMessages.add(olderMessage);

        when(aiMessageRepository.findHistoryMessages(eq(7L), any(), any(), eq(null), eq(null)))
                .thenReturn(fetchedMessages);
        when(aiResponseSourceRepository.findAllByMessageIds(messageIds))
                .thenReturn(List.of());

        var result = service.getHistoryMessages(1L, null, null);

        assertThat(result.hasNext()).isTrue();
        assertThat(result.messages()).hasSize(1);
        assertThat(result.messages().getFirst().date().toString()).isEqualTo("2026-07-06");
        assertThat(result.messages().getFirst().items()).hasSize(21);
        assertThat(result.nextCursor().id()).isEqualTo(280L);
        assertThat(result.nextCursor().createdAt()).isEqualTo(Instant.parse("2026-07-06T01:00:00Z"));
    }
}
