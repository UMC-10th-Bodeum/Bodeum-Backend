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
    COMMENT_CONTENT_TOO_LONG(
            HttpStatus.BAD_REQUEST,
            "COMMUNITY400_5",
            "댓글 내용은 1,000자를 초과할 수 없습니다."
    ),
    COMMENT_CONTENT_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "COMMUNITY400_6",
            "댓글 내용은 비어 있을 수 없습니다."
    ),
    INVALID_POST_LIST_SORT(
            HttpStatus.BAD_REQUEST,
            "COMMUNITY400_7",
            "게시글 정렬 기준은 latest, view, like, comment 중 하나여야 합니다."
    ),
    POST_NOT_QUESTION(
            HttpStatus.BAD_REQUEST,
            "COMMUNITY400_8",
            "정보/질문 광장 게시글의 댓글만 채택할 수 있습니다."
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
    COMMENT_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "COMMUNITY403_2",
            "댓글을 수정하거나 삭제할 권한이 없습니다."
    ),
    COMMENT_ADOPTION_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "COMMUNITY403_3",
            "게시글 작성자만 댓글의 채택 상태를 변경할 수 있습니다."
    ),
    POST_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "COMMUNITY404_1",
            "게시글을 찾을 수 없습니다."
    ),
    COMMENT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "COMMUNITY404_2",
            "댓글을 찾을 수 없습니다."
    ),
    COMMENT_ALREADY_ADOPTED(
            HttpStatus.CONFLICT,
            "COMMUNITY409_1",
            "이미 채택된 댓글이 있습니다."
    ),
    POST_BOARD_CHANGE_BLOCKED_BY_ADOPTED_COMMENT(
            HttpStatus.CONFLICT,
            "COMMUNITY409_2",
            "채택된 댓글이 있는 질문글은 다른 게시판으로 변경할 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
