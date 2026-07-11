package com.bodeum.domain.onboarding.dto.response;

import com.bodeum.domain.auth.enumtype.AuthNextStep;
import com.bodeum.domain.user.entity.UserAccount;

public record OnboardingStatusResponse(
        boolean childProfileRegistered,
        boolean interestRegionRegistered,
        boolean guardianProfileRegistered,
        boolean onboardingCompleted,
        AuthNextStep nextStep
) {

    public static OnboardingStatusResponse from(UserAccount userAccount) {
        return new OnboardingStatusResponse(
                userAccount.isChildProfileRegistered(),
                userAccount.isInterestRegionRegistered(),
                userAccount.isGuardianProfileRegistered(),
                userAccount.isOnboardingCompleted(),
                userAccount.isOnboardingResolved() ? AuthNextStep.HOME : AuthNextStep.ONBOARDING
        );
    }
}
