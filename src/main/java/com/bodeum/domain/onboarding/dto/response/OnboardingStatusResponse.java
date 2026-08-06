package com.bodeum.domain.onboarding.dto.response;

import com.bodeum.domain.auth.enums.AuthNextStep;
import com.bodeum.domain.user.entity.User;

public record OnboardingStatusResponse(
        boolean childProfileRegistered,
        boolean interestRegionRegistered,
        boolean guardianProfileRegistered,
        boolean onboardingCompleted,
        AuthNextStep nextStep
) {

    public static OnboardingStatusResponse from(User user) {
        return new OnboardingStatusResponse(
                user.isChildProfileRegistered(),
                user.isInterestRegionRegistered(),
                user.isGuardianProfileRegistered(),
                user.isOnboardingCompleted(),
                user.isOnboardingResolved() ? AuthNextStep.HOME : AuthNextStep.ONBOARDING
        );
    }
}
