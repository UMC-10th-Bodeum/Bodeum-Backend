package com.bodeum.domain.onboarding.enumtype;

import com.bodeum.domain.onboarding.exception.OnboardingErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.Arrays;

public enum GuardianType {

    PARENT,
    GRANDPARENT,
    SIBLING,
    OTHER;

    public static GuardianType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new ProjectException(OnboardingErrorCode.INVALID_GUARDIAN_TYPE));
    }

    public static GuardianType fromNullable(String value) {
        return value == null || value.isBlank() ? null : from(value);
    }
}
