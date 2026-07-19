package com.bodeum.domain.user.exception;

import com.bodeum.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    USER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "USER404_1",
            "사용자를 찾을 수 없습니다."
    ),
    USER_AGREEMENT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "USER404_2",
            "사용자 약관 동의 정보를 찾을 수 없습니다."
    ),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}