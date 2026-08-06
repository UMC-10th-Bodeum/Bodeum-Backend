package com.bodeum.domain.ai.dto.response;

import com.bodeum.domain.ai.enums.AiFeedbackReasonType;
import com.bodeum.domain.ai.enums.AiFeedbackType;
import java.util.List;
import java.util.Objects;

public record AiMessageFeedbackResponse(
        Long aiFeedbackId,
        AiFeedbackType feedbackType,
        List<AiFeedbackReasonType> reasons
) {

    public AiMessageFeedbackResponse {
        Objects.requireNonNull(aiFeedbackId, "aiFeedbackId must not be null");
        Objects.requireNonNull(feedbackType, "feedbackType must not be null");
        reasons = feedbackType == AiFeedbackType.INCORRECT
                ? List.copyOf(Objects.requireNonNull(reasons,
                        "INCORRECT feedback reasons must not be null"))
                : null;
    }
}
