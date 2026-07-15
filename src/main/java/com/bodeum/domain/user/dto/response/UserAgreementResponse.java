package com.bodeum.domain.user.dto.response;

import com.bodeum.domain.auth.enumtype.AuthNextStep;
import com.bodeum.domain.user.entity.User;
import java.time.Instant;

public record UserAgreementResponse(
        boolean serviceTermsAgreed,
        boolean privacyPolicyAgreed,
        boolean aiTermsAgreed,
        Instant agreedAt,
        AuthNextStep nextStep
) {

    public static UserAgreementResponse from(User user) {
        return new UserAgreementResponse(
                user.isServiceTermsAgreed(),
                user.isPrivacyPolicyAgreed(),
                user.isAiTermsAgreed(),
                user.getAgreementAgreedAt(),
                user.isOnboardingResolved() ? AuthNextStep.HOME : AuthNextStep.ONBOARDING
        );
    }
}
