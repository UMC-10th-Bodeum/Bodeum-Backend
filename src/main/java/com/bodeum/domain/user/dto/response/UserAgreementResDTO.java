package com.bodeum.domain.user.dto.response;

import com.bodeum.domain.auth.enumtype.AuthNextStep;
import com.bodeum.domain.user.model.UserAccount;
import java.time.LocalDateTime;

public record UserAgreementResDTO(
        boolean serviceTermsAgreed,
        boolean privacyPolicyAgreed,
        boolean marketingAgreed,
        LocalDateTime agreedAt,
        AuthNextStep nextStep
) {

    public static UserAgreementResDTO from(UserAccount userAccount) {
        return new UserAgreementResDTO(
                userAccount.isServiceTermsAgreed(),
                userAccount.isPrivacyPolicyAgreed(),
                userAccount.isMarketingAgreed(),
                userAccount.getAgreementAgreedAt(),
                AuthNextStep.ONBOARDING
        );
    }
}
