package com.bodeum.domain.ai.dto.response;

import com.bodeum.domain.ai.enums.AiFeedbackReasonType;
import com.bodeum.domain.ai.enums.AiFeedbackType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record CreateAiFeedbackResponse(
        Long aiFeedbackId,
        AiFeedbackType feedbackType,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<AiFeedbackReasonType> reasons
) {

    public static CreateAiFeedbackResponse of(
            Long aiFeedbackId,
            AiFeedbackType feedbackType,
            List<AiFeedbackReasonType> reasons
    ) {
        return new CreateAiFeedbackResponse(
                aiFeedbackId,
                feedbackType,
                feedbackType == AiFeedbackType.INCORRECT ? List.copyOf(reasons) : null
        );
    }
}
