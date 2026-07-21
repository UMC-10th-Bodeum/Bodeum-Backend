package com.bodeum.domain.ai.dto.response;

import com.bodeum.domain.ai.enums.AiWarningType;

public record AiMessageWarningResponse(
        AiWarningType type,
        String message
) {
}
