package com.bodeum.domain.onboarding.dto.response;

import com.bodeum.domain.auth.enumtype.AuthNextStep;

public record OnboardingStepResDTO(
        String completedStep,
        boolean onboardingCompleted,
        AuthNextStep nextStep
) {

    public static OnboardingStepResDTO of(String completedStep, boolean onboardingCompleted) {
        return new OnboardingStepResDTO(
                completedStep,
                onboardingCompleted,
                onboardingCompleted ? AuthNextStep.HOME : AuthNextStep.ONBOARDING
        );
    }
}
