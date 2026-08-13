package com.bodeum.domain.ai.model.context;

public record AiConversationContext(
        String previousUserQuestion,
        String previousAiAnswer,
        String immediatePreviousUserQuestion,
        AiResolvedContext immediatePreviousResolvedContext,
        Long parentUserMessageId,
        Long rootUserMessageId
) {
    public static AiConversationContext empty() {
        return new AiConversationContext(null, null, null, null, null, null);
    }

    public boolean hasContext() {
        return previousUserQuestion != null
                && !previousUserQuestion.isBlank()
                && previousAiAnswer != null
                && !previousAiAnswer.isBlank();
    }
}
