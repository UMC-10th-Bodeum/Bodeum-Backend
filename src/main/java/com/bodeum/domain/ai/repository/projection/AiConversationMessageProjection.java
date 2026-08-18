package com.bodeum.domain.ai.repository.projection;

import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.model.context.AiResolvedContext;

public interface AiConversationMessageProjection {

    Long getId();

    SenderType getSenderType();

    String getContent();

    String getResolvedQuestion();

    AiResolvedContext getResolvedContext();

    Long getContextRootMessageId();
}
