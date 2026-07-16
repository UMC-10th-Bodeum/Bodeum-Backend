package com.bodeum.domain.ai.repository.projection;

import java.time.Instant;

public interface AiResponseSourceProjection {

    Long getAiMessageId();

    String getSourceTitle();

    String getSourceUrl();

    Instant getSourceUpdatedAt();
}
