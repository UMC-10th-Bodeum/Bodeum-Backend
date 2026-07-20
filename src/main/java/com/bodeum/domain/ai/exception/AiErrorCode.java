package com.bodeum.domain.ai.exception;

import com.bodeum.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements BaseErrorCode {

    AI_CHAT_ROOM_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "AI404_1",
            "AI 채팅방을 찾을 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}