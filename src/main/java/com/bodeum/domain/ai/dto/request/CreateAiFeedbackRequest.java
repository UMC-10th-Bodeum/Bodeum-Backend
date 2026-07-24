package com.bodeum.domain.ai.dto.request;

import com.bodeum.domain.ai.enums.AiFeedbackReasonType;
import com.bodeum.domain.ai.enums.AiFeedbackType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateAiFeedbackRequest(
        @NotNull AiFeedbackType feedbackType,
        List<AiFeedbackReasonType> reasons
) {

    @AssertTrue(message = "HELPFUL은 사유 없이, INCORRECT는 중복되지 않은 사유를 1개 이상 전송해야 합니다.")
    public boolean isValidReasons() {
        if (feedbackType == null) {
            return true;
        }
        return switch (feedbackType) {
            case HELPFUL -> reasons == null || reasons.isEmpty();
            case INCORRECT -> reasons != null
                    && !reasons.isEmpty()
                    && reasons.stream().distinct().count() == reasons.size();
        };
    }
}
