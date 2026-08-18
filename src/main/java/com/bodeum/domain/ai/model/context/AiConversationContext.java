package com.bodeum.domain.ai.model.context;

public record AiConversationContext(
        String recentConversation,
        String previousUserQuestion,
        String previousAiAnswer,
        String immediatePreviousUserQuestion,
        AiResolvedContext immediatePreviousResolvedContext,
        Long parentUserMessageId,
        Long rootUserMessageId
) {
    public static AiConversationContext empty() {
        return new AiConversationContext(null, null, null, null, null, null, null);
    }

    public boolean hasContext() {
        return recentConversation != null && !recentConversation.isBlank();
    }
}
