package com.bodeum.domain.onboarding.enumtype;

import com.bodeum.domain.onboarding.exception.OnboardingErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.Arrays;

public enum CareArea {

    AUTISM_SPECTRUM,
    INTELLECTUAL,
    BRAIN_LESION,
    ADHD,
    DEVELOPMENTAL,
    LANGUAGE,
    OTHER;

    public static CareArea from(String value) {
        return Arrays.stream(values())
                .filter(area -> area.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new ProjectException(OnboardingErrorCode.INVALID_CARE_AREA));
    }
}
