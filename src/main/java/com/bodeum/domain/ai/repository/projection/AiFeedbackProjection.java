package com.bodeum.domain.ai.repository.projection;

import com.bodeum.domain.ai.enums.AiFeedbackReasonType;
import com.bodeum.domain.ai.enums.AiFeedbackType;

public interface AiFeedbackProjection {

    Long getAiMessageId();

    Long getAiFeedbackId();

    AiFeedbackType getFeedbackType();

    AiFeedbackReasonType getReason();
}
