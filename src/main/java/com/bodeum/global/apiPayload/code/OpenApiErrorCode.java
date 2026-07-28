package com.bodeum.global.apiPayload.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OpenApiErrorCode implements BaseErrorCode {

    // 400
    INVALID_SOURCE_SPEC(HttpStatus.BAD_REQUEST, "OPENAPI400_1", "유효하지 않은 오픈 API 소스 명세입니다."),
    EMPTY_RESPONSE_DATA(HttpStatus.BAD_REQUEST, "OPENAPI400_2", "외부 기관으로부터 수집된 데이터가 비어있습니다."),
    DATA_PARSING_FAILED(HttpStatus.BAD_REQUEST, "OPENAPI400_3", "JSON/XML 데이터 정규화 및 파싱에 실패했습니다."),

    // 500
    EXTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "OPENAPI500_1", "외부 공공데이터 기관 서버가 응답하지 않거나 에러를 반환했습니다."),
    BULK_INSERT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OPENAPI500_2", "수집 데이터 복사 및 벌크 데이터베이스 저장 중 결함이 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}