package com.bodeum.domain.ai.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.ai.enums.AiResponseProcessingStatus;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.model.context.AiResolvedContext;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AiMessageTest {

    @Test
    void completesProcessingStatusOnlyForUserMessage() {
        AiMessage userMessage = AiMessage.createUserMessage(null, "질문");

        assertThat(userMessage.getAiProcessingStatus())
                .isEqualTo(AiResponseProcessingStatus.PROCESSING);

        userMessage.completeAiResponse();

        assertThat(userMessage.getAiProcessingStatus())
                .isEqualTo(AiResponseProcessingStatus.COMPLETED);
    }

    @Test
    void marksProcessingUserMessageAsFailed() {
        AiMessage userMessage = AiMessage.createUserMessage(null, "질문");

        userMessage.failAiResponse();

        assertThat(userMessage.getAiProcessingStatus())
                .isEqualTo(AiResponseProcessingStatus.FAILED);
    }

    @Test
    void keepsAiMessageProcessingStatusNull() {
        AiMessage aiMessage = AiMessage.createAiMessage(
                null, "답변", false, AiAnswerStatus.ANSWERED);

        assertThat(aiMessage.getAiProcessingStatus()).isNull();
        assertThat(aiMessage.getAiAnswerStatus()).isEqualTo(AiAnswerStatus.ANSWERED);
    }

    @Test
    void storesResolvedQuestionAndContextParentForUserMessage() {
        AiMessage userMessage = AiMessage.createUserMessage(
                null,
                "그중에서 공립만 알려줘"
        );

        AiResolvedContext resolvedContext = new AiResolvedContext(
                "특수학교",
                new AiResolvedContext.RegionContext("경기도", "수원시"),
                Map.of("설립구분", "공립"),
                "목록",
                null
        );
        userMessage.updateConversationContext(
                "수원시 특수학교 중 공립 학교만 알려줘",
                resolvedContext,
                100L,
                90L
        );
        ReflectionTestUtils.setField(userMessage, "id", 102L);

        assertThat(userMessage.getResolvedQuestion())
                .isEqualTo("수원시 특수학교 중 공립 학교만 알려줘");
        assertThat(userMessage.getContextParentMessageId()).isEqualTo(100L);
        assertThat(userMessage.getContextRootMessageId()).isEqualTo(90L);
        assertThat(userMessage.getResolvedContext()).isEqualTo(resolvedContext);
    }

    @Test
    void aiMessageInheritsConversationContextFromUserMessage() {
        AiMessage userMessage = AiMessage.createUserMessage(null, "5개 더 알려줘");
        AiResolvedContext resolvedContext = new AiResolvedContext(
                "재활센터", null, Map.of(), "목록", 5);
        userMessage.updateConversationContext(
                "수원시 재활센터 중 이전 기관을 제외하고 5개 알려줘",
                resolvedContext,
                100L,
                90L
        );
        ReflectionTestUtils.setField(userMessage, "id", 102L);
        AiMessage aiMessage = AiMessage.createAiMessage(
                null,
                "추가 기관 안내",
                false,
                AiAnswerStatus.ANSWERED
        );

        aiMessage.inheritConversationContext(userMessage);

        assertThat(aiMessage.getContextRootMessageId()).isEqualTo(90L);
        assertThat(aiMessage.getContextParentMessageId()).isEqualTo(102L);
        assertThat(aiMessage.getResolvedContext()).isEqualTo(resolvedContext);
    }
}
