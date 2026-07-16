package com.bodeum.domain.onboarding.enums;

import com.bodeum.domain.onboarding.exception.OnboardingErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.Arrays;

public enum CommunityRoleType {

    INFO_SEEKER,
    EXPERIENCE_SHARER,
    WISDOM_HELPER;

    public static CommunityRoleType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new ProjectException(OnboardingErrorCode.INVALID_COMMUNITY_ROLE_TYPE));
    }

    public static CommunityRoleType fromNullable(String value) {
        return value == null || value.isBlank() ? null : from(value);
    }
}
