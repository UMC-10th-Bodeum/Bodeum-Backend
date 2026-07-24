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
    AI_INVALID_HISTORY_CURSOR(
            HttpStatus.BAD_REQUEST,
            "AI400_1",
            "이전 대화 조회 커서 값이 올바르지 않습니다."
    ),
    AI_FEEDBACK_AI_MESSAGE_ONLY(
            HttpStatus.BAD_REQUEST,
            "AI400_2",
            "AI 메시지에만 피드백을 등록할 수 있습니다."
    ),
    AI_MESSAGE_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "AI403_2",
            "해당 AI 메시지에 접근할 수 없습니다."
    ),
    AI_MESSAGE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "AI404_2",
            "AI 메시지를 찾을 수 없습니다."
    ),
    ALREADY_FEEDBACK(
            HttpStatus.CONFLICT,
            "ALREADY_FEEDBACK",
            "이미 피드백한 메시지입니다."
    ),
    AI_FEEDBACK_TEMPORARILY_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "AI_FEEDBACK_TEMPORARILY_UNAVAILABLE",
            "피드백 요청이 처리 중입니다. 잠시 후 다시 시도해 주세요."
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
            "AI 질문은 너무 자주 요청할 수 없습니다. 잠시 후 다시 시도해 주세요."
    ),
    AI_DAILY_LIMIT_EXCEEDED(
            HttpStatus.TOO_MANY_REQUESTS,
            "AI_DAILY_LIMIT_EXCEEDED",
            "오늘 사용할 수 있는 AI 질문 횟수를 모두 사용했습니다."
    ),
    AI_REQUEST_IN_PROGRESS(
            HttpStatus.CONFLICT,
            "AI_REQUEST_IN_PROGRESS",
            "이미 AI 응답을 생성하고 있습니다. 기존 요청이 끝난 뒤 다시 시도해 주세요."
    ),
    AI_INDEXING_FAILED(
            HttpStatus.SERVICE_UNAVAILABLE,
            "AI_INDEXING_FAILED",
            "AI 색인 작업에 실패했습니다."
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
