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
        boolean onboardingCompleted = userAccount.isOnboardingCompleted();
        return new OnboardingStatusResponse(
                userAccount.isChildProfileRegistered(),
                userAccount.isInterestRegionRegistered(),
                userAccount.isGuardianProfileRegistered(),
                onboardingCompleted,
                onboardingCompleted ? AuthNextStep.HOME : AuthNextStep.ONBOARDING
        );
    }
}
