package com.bodeum.domain.auth.exception;

import com.bodeum.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH400_1", "지원하지 않는 소셜 로그인 제공자입니다."),
    PROVIDER_NOT_CONFIGURED(HttpStatus.BAD_REQUEST, "AUTH400_2", "소셜 로그인 설정이 완료되지 않았습니다."),
    MISSING_AUTH_CODE(HttpStatus.BAD_REQUEST, "AUTH400_3", "인가 코드가 존재하지 않습니다."),
    INVALID_OAUTH_STATE(HttpStatus.UNAUTHORIZED, "AUTH401_1", "유효하지 않은 OAuth state 값입니다."),
    SOCIAL_TOKEN_EXCHANGE_FAILED(HttpStatus.UNAUTHORIZED, "AUTH401_2", "소셜 액세스 토큰 발급에 실패했습니다."),
    SOCIAL_PROFILE_FETCH_FAILED(HttpStatus.UNAUTHORIZED, "AUTH401_3", "소셜 사용자 정보 조회에 실패했습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_4", "유효하지 않은 리프레시 토큰입니다."),
    INACTIVE_USER(HttpStatus.UNAUTHORIZED, "AUTH401_5", "탈퇴했거나 비활성화된 사용자입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
