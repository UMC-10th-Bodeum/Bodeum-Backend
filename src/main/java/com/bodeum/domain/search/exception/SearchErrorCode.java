package com.bodeum.domain.search.exception;

import com.bodeum.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SearchErrorCode implements BaseErrorCode {

    KEYWORD_TOO_SHORT(
            HttpStatus.BAD_REQUEST,
            "SEARCH400_1",
            "두 글자 이상 검색해주세요."
    ),
    SEARCH_HISTORY_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "SEARCH404_1",
            "해당 검색어 기록을 찾을 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
