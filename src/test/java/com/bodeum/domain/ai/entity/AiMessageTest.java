package com.bodeum.domain.ai.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.ai.enums.AiResponseProcessingStatus;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import org.junit.jupiter.api.Test;

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
}
