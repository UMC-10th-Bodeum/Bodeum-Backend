package com.bodeum.domain.community.exception;

import com.bodeum.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommunityErrorCode implements BaseErrorCode {

    POST_TITLE_TOO_LONG(
            HttpStatus.BAD_REQUEST,
            "COMMUNITY400_1",
            "게시글 제목은 150자를 초과할 수 없습니다."
    ),
    POST_CONTENT_TOO_LONG(
            HttpStatus.BAD_REQUEST,
            "COMMUNITY400_2",
            "게시글 내용은 2,000자를 초과할 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
