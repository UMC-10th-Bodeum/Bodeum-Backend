package com.bodeum.domain.ai.service.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.model.context.AiResolvedContext;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.ai.repository.AiResponseSourceRepository;
import com.bodeum.domain.ai.repository.AiSourceReviewRepository;
import com.bodeum.domain.ai.repository.projection.AiConversationMessageProjection;
import com.bodeum.domain.ai.service.validation.AiAnswerEvidenceService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class AiConversationContextServiceTest {

    private final AiMessageRepository messageRepository = mock(AiMessageRepository.class);
    private final AiResponseSourceRepository sourceRepository =
            mock(AiResponseSourceRepository.class);
    private final AiConversationContextService service = new AiConversationContextService(
            messageRepository,
            sourceRepository,
            new AiAnswerEvidenceService(mock(AiSourceReviewRepository.class)));

    @Test
    void buildsRecentContextWithOneProjectionQuery() {
        ReflectionTestUtils.setField(service, "recentConversationTurnCount", 3);
        AiResolvedContext resolvedContext = new AiResolvedContext(
                "특수학교", null, java.util.Map.of(), "목록", 5);
        List<AiConversationMessageProjection> messages = List.of(
                message(4L, SenderType.USER, "더 알려줘", null, null, null),
                message(3L, SenderType.AI, "학교 세 곳을 안내했습니다.",
                        null, null, 1L),
                message(2L, SenderType.USER, "특수학교 알려줘",
                        "수원 특수학교 알려줘", resolvedContext, 1L),
                message(1L, SenderType.AI, "이전 답변", null, null, 1L)
        );
        when(messageRepository.findRecentConversationContext(eq(1L), any(Pageable.class)))
                .thenReturn(messages);

        var context = service.resolve(1L);

        assertThat(context.immediatePreviousUserQuestion())
                .isEqualTo("수원 특수학교 알려줘");
        assertThat(context.previousAiAnswer()).isEqualTo("학교 세 곳을 안내했습니다.");
        assertThat(context.recentConversation()).isEqualTo("""
                사용자: 수원 특수학교 알려줘
                AI: 학교 세 곳을 안내했습니다.""");
        assertThat(context.immediatePreviousResolvedContext()).isEqualTo(resolvedContext);
        assertThat(context.rootUserMessageId()).isEqualTo(1L);
        verify(messageRepository, never())
                .findByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                        any(), any(), any());
    }

    private AiConversationMessageProjection message(
            Long id,
            SenderType senderType,
            String content,
            String resolvedQuestion,
            AiResolvedContext resolvedContext,
            Long rootId
    ) {
        AiConversationMessageProjection message = mock(AiConversationMessageProjection.class);
        when(message.getId()).thenReturn(id);
        when(message.getSenderType()).thenReturn(senderType);
        when(message.getContent()).thenReturn(content);
        when(message.getResolvedQuestion()).thenReturn(resolvedQuestion);
        when(message.getResolvedContext()).thenReturn(resolvedContext);
        when(message.getContextRootMessageId()).thenReturn(rootId);
        return message;
    }
}
