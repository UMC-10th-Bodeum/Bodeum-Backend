package com.bodeum.domain.term.exception;

import com.bodeum.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TermsErrorCode implements BaseErrorCode {

    UNSUPPORTED_TYPE(HttpStatus.BAD_REQUEST, "TERMS400_1", "지원하지 않는 약관 유형입니다."),
    CONTENT_LOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "TERMS500_1", "약관을 불러올 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
