package com.bodeum.domain.onboarding.exception;

import com.bodeum.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OnboardingErrorCode implements BaseErrorCode {

    INVALID_GUARDIAN_TYPE(HttpStatus.BAD_REQUEST, "ONBOARDING400_1", "지원하지 않는 보호자 유형입니다."),
    INVALID_COMMUNITY_ROLE_TYPE(HttpStatus.BAD_REQUEST, "ONBOARDING400_2", "지원하지 않는 커뮤니티 역할 성향입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
