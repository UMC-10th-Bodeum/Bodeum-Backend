package com.bodeum.domain.ai.exception;

import com.bodeum.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements BaseErrorCode {

    AI_TERMS_NOT_AGREED(
            HttpStatus.FORBIDDEN,
            "AI403_1",
            "AI 챗봇 이용동의가 필요합니다."
    ),
    AI_CHAT_ROOM_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "AI404_1",
            "AI 채팅방을 찾을 수 없습니다."
    ),
    AI_RESPONSE_FAILED(
            HttpStatus.SERVICE_UNAVAILABLE,
            "AI_RESPONSE_FAILED",
            "AI 응답 생성에 실패했습니다."
    ),
    AI_RESPONSE_TIMEOUT(
            HttpStatus.SERVICE_UNAVAILABLE,
            "AI_RESPONSE_TIMEOUT",
            "AI 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요."
    ),
    AI_RATE_LIMIT_EXCEEDED(
            HttpStatus.TOO_MANY_REQUESTS,
            "AI_RATE_LIMIT_EXCEEDED",
            "AI 질문을 너무 자주 요청했습니다. 잠시 후 다시 시도해 주세요."
    ),
    AI_DAILY_LIMIT_EXCEEDED(
            HttpStatus.TOO_MANY_REQUESTS,
            "AI_DAILY_LIMIT_EXCEEDED",
            "오늘 사용할 수 있는 AI 질문 횟수를 모두 사용했습니다."
    ),
    AI_REQUEST_IN_PROGRESS(
            HttpStatus.CONFLICT,
            "AI_REQUEST_IN_PROGRESS",
            "이미 AI 답변을 생성하고 있습니다. 기존 요청이 끝난 후 다시 시도해 주세요."
    ),
    AI_INDEXING_FAILED(
            HttpStatus.SERVICE_UNAVAILABLE,
            "AI_INDEXING_FAILED",
            "AI 검색 자료 저장에 실패했습니다."
    ),
    AI_INVALID_SOURCE_METADATA(
            HttpStatus.SERVICE_UNAVAILABLE,
            "AI_SOURCE_INVALID",
            "AI 참고자료를 확인할 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
