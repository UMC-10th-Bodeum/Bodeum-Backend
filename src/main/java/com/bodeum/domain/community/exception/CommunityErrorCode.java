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
    ),
    POST_TITLE_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "COMMUNITY400_3",
            "게시글 제목은 비어 있을 수 없습니다."
    ),
    POST_CONTENT_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "COMMUNITY400_4",
            "게시글 내용은 비어 있을 수 없습니다."
    ),
    AUTHENTICATION_REQUIRED(
            HttpStatus.UNAUTHORIZED,
            "COMMUNITY401_1",
            "로그인이 필요한 요청입니다."
    ),
    POST_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "COMMUNITY403_1",
            "게시글을 수정하거나 삭제할 권한이 없습니다."
    ),
    POST_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "COMMUNITY404_1",
            "게시글을 찾을 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
