package com.bodeum.domain.region.exception;

import com.bodeum.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RegionErrorCode implements BaseErrorCode {

    REGION_NOT_FOUND(HttpStatus.BAD_REQUEST, "REGION400_1", "존재하지 않는 지역입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
