package com.bodeum.domain.onboarding.enumtype;

import com.bodeum.domain.onboarding.exception.OnboardingErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.Arrays;

public enum InterestCategory {

    WELFARE_SUBSIDY,
    HOSPITAL_HEALTH,
    PARENTING_COMMUNICATION,
    GROWTH_EDUCATION;

    public static InterestCategory from(String value) {
        return Arrays.stream(values())
                .filter(category -> category.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new ProjectException(OnboardingErrorCode.INVALID_INTEREST_CATEGORY));
    }
}
