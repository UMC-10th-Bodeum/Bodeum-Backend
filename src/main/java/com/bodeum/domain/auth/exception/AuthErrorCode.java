package com.bodeum.domain.auth.exception;

import com.bodeum.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH400_1", "지원하지 않는 소셜 로그인 제공자입니다."),
    MISSING_AUTH_CODE(HttpStatus.BAD_REQUEST, "AUTH400_2", "유효하지 않은 인가 코드입니다."),
    ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, "AUTH400_3", "이미 탈퇴한 사용자입니다."),
    REQUIRED_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "AUTH400_4", "필수 약관에 동의해주세요."),
    SOCIAL_TOKEN_EXCHANGE_FAILED(HttpStatus.BAD_REQUEST, "AUTH400_5", "소셜 토큰 교환에 실패했습니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_1", "유효하지 않은 토큰입니다."),
    SOCIAL_PROFILE_FETCH_FAILED(HttpStatus.UNAUTHORIZED, "AUTH401_2", "소셜 인증에 실패했습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_3", "유효하지 않은 refreshToken입니다. 다시 로그인해주세요."),
    INACTIVE_USER(HttpStatus.UNAUTHORIZED, "AUTH401_5", "탈퇴했거나 비활성화된 사용자입니다."),
    INVALID_OAUTH_STATE(HttpStatus.UNAUTHORIZED, "AUTH401_6", "유효하지 않은 OAuth state 값입니다."),
    SIGNUP_NOT_COMPLETED(HttpStatus.FORBIDDEN, "AUTH403_1", "온보딩을 완료해야 이용할 수 있는 기능입니다."),
    PROVIDER_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH500_1", "소셜 로그인 설정이 완료되지 않았습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
