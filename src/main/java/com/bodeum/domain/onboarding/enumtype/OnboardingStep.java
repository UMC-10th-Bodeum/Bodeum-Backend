package com.bodeum.domain.onboarding.enumtype;

public enum OnboardingStep {

    CHILD_PROFILE(1),
    INTEREST_REGION(2),
    GUARDIAN_PROFILE(3);

    private final int step;

    OnboardingStep(int step) {
        this.step = step;
    }

    public int getStep() {
        return step;
    }
}
