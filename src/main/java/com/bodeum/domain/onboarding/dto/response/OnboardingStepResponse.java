package com.bodeum.domain.onboarding.dto.response;

import com.bodeum.domain.auth.enums.AuthNextStep;
import com.bodeum.domain.onboarding.enums.OnboardingStep;

public record OnboardingStepResponse(
        int step,
        OnboardingStep completedStep,
        boolean onboardingCompleted,
        AuthNextStep nextStep
) {

    public static OnboardingStepResponse of(OnboardingStep completedStep, boolean onboardingCompleted) {
        return new OnboardingStepResponse(
                completedStep.getStep(),
                completedStep,
                onboardingCompleted,
                onboardingCompleted ? AuthNextStep.HOME : AuthNextStep.ONBOARDING
        );
    }
}
