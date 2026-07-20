package com.bodeum.domain.ai.dto.response;

import java.time.Instant;

public record AiGuideConfirmationResponse(
        Instant lastGuideConfirmedAt
) {

    public static AiGuideConfirmationResponse of(
            Instant lastGuideConfirmedAt
    ) {
        return new AiGuideConfirmationResponse(lastGuideConfirmedAt);
    }
}
