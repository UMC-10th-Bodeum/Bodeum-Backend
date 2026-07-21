package com.bodeum.domain.ai.repository.projection;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import java.time.Instant;

public interface AiResponseSourceProjection {

    Long getAiMessageId();

    AiResponseSourceType getSourceType();

    Long getSourceId();

    String getSourceTitle();

    String getSourceUrl();

    Instant getSourceUpdatedAt();
}
