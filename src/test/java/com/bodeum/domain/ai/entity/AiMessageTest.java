package com.bodeum.domain.ai.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.ai.enums.AiResponseProcessingStatus;
import org.junit.jupiter.api.Test;

class AiMessageTest {

    @Test
    void completesProcessingStatusOnlyForUserMessage() {
        AiMessage userMessage = AiMessage.createUserMessage(null, "질문");

        assertThat(userMessage.getAiResponseStatus())
                .isEqualTo(AiResponseProcessingStatus.PROCESSING);

        userMessage.completeAiResponse();

        assertThat(userMessage.getAiResponseStatus())
                .isEqualTo(AiResponseProcessingStatus.COMPLETED);
    }

    @Test
    void marksProcessingUserMessageAsFailed() {
        AiMessage userMessage = AiMessage.createUserMessage(null, "질문");

        userMessage.failAiResponse();

        assertThat(userMessage.getAiResponseStatus())
                .isEqualTo(AiResponseProcessingStatus.FAILED);
    }

    @Test
    void keepsAiMessageProcessingStatusNull() {
        AiMessage aiMessage = AiMessage.createAiMessage(null, "답변", false);

        assertThat(aiMessage.getAiResponseStatus()).isNull();
    }
}
