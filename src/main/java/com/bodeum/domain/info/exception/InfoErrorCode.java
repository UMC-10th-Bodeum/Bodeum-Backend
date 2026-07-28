package com.bodeum.domain.info.exception;

import com.bodeum.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InfoErrorCode implements BaseErrorCode {

    // 400 Bad Request
    INVALID_ID_FORMAT(HttpStatus.BAD_REQUEST, "INFO400_2", "잘못된 요청입니다. 올바른 식별자 형식이 아닙니다."),
    RATING_REQUIRED(HttpStatus.BAD_REQUEST, "INFO400_3", "잘못된 요청입니다. 별점을 남겨주세요 (1~5 정수)."),
    CANNOT_HELPFUL_OWN_REVIEW(HttpStatus.BAD_REQUEST, "INFO400_4", "잘못된 요청입니다. 본인이 작성한 후기에는 '도움돼요'를 누를 수 없습니다."),
    CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "INFO400_5", "잘못된 요청입니다. 후기 본문을 작성해주세요."),
    CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "INFO400_6", "잘못된 요청입니다. 본문은 최대 2000자까지 작성 가능합니다."),

    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "INFO401", "인증에 실패했습니다. 로그인이 필요한 서비스입니다."),

    // 403 Forbidden
    FORBIDDEN_REVIEW_DELETE(HttpStatus.FORBIDDEN, "INFO403_1", "권한이 없습니다. 본인이 작성한 후기만 삭제할 수 있습니다."),
    FORBIDDEN_REVIEW_UPDATE(HttpStatus.FORBIDDEN, "INFO403_2", "권한이 없습니다. 본인이 작성한 후기만 수정할 수 있습니다."),

    // 404 Not Found
    INFO_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "INFO404_1", "해당 리소스를 찾을 수 없어 스크랩 처리를 완료할 수 없습니다."),
    INFO_REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "INFO404_2", "해당 리소스를 찾을 수 없어 도움돼요 처리를 완료할 수 없습니다.");
  
    INFO_SHARE_LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "INFO0404_1", "해당 리소스를 찾을 수 없어 공유 링크 생성이 불가능합니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;
}