package com.bodeum.domain.ai.dto.response;

import java.util.List;

public record AiChatStarterResponse(
        String greeting,
        List<String> suggestedQuestions
) {

    public AiChatStarterResponse {
        suggestedQuestions = List.copyOf(suggestedQuestions);
    }

    public static AiChatStarterResponse of(
            String greeting,
            List<String> suggestedQuestions
    ) {
        return new AiChatStarterResponse(greeting, suggestedQuestions);
    }
}
