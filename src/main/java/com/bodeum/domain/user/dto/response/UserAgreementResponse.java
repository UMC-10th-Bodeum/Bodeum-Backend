package com.bodeum.domain.user.dto.response;

import com.bodeum.domain.auth.enumtype.AuthNextStep;
import com.bodeum.domain.user.entity.UserAccount;
import java.time.LocalDateTime;

public record UserAgreementResponse(
        boolean serviceTermsAgreed,
        boolean privacyPolicyAgreed,
        boolean aiChatAgreed,
        LocalDateTime agreedAt,
        AuthNextStep nextStep
) {

    public static UserAgreementResponse from(UserAccount userAccount) {
        return new UserAgreementResponse(
                userAccount.isServiceTermsAgreed(),
                userAccount.isPrivacyPolicyAgreed(),
                userAccount.isAiChatAgreed(),
                userAccount.getAgreementAgreedAt(),
                userAccount.isOnboardingResolved() ? AuthNextStep.HOME : AuthNextStep.ONBOARDING
        );
    }
}
