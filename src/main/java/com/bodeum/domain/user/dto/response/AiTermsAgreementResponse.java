package com.bodeum.domain.user.dto.response;

import java.time.Instant;

public record AiTermsAgreementResponse(
        boolean aiTermsAgreed,
        Instant agreedAt
) {

    public static AiTermsAgreementResponse of(
            boolean aiTermsAgreed,
            Instant agreedAt
    ) {
        return new AiTermsAgreementResponse(
                aiTermsAgreed,
                agreedAt
        );
    }
}
