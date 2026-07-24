package com.bodeum.domain.ai.dto.response;

import com.bodeum.domain.ai.enums.AiWarningType;

public record AiMessageWarningResponse(
        AiWarningType type,
        String message
) {

    private static final String INCORRECT_SOURCE_MESSAGE =
            "일부 사용자로부터 오류 피드백이 접수된 정보입니다. "
                    + "정확한 내용은 공식 기관에서 다시 확인해 주세요.";

    public static AiMessageWarningResponse incorrectSource() {
        return new AiMessageWarningResponse(
                AiWarningType.INCORRECT_SOURCE,
                INCORRECT_SOURCE_MESSAGE
        );
    }
}
