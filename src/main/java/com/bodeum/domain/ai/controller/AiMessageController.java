package com.bodeum.domain.ai.controller;

import com.bodeum.domain.ai.dto.response.AiMessageHistoryResponse;
import com.bodeum.domain.ai.dto.response.AiTodayMessageResponse;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.service.AiMessageQueryService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI message", description = "AI 챗봇 대화 이력 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai/messages")
public class AiMessageController {

    private final AiMessageQueryService aiMessageQueryService;

    @Operation(
            summary = "오늘 AI 대화 이력 조회",
            description = "현재 로그인한 사용자의 오늘 AI 대화 이력을 조회한다."
    )
    @GetMapping("/today")
    public ApiResponse<AiTodayMessageResponse> getTodayMessages(
            @LoginUser Long userId
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                aiMessageQueryService.getTodayMessages(userId)
        );
    }

    @Operation(
            summary = "이전 AI 대화 이력 조회",
            description = "현재 로그인한 사용자의 오늘 이전 AI 대화 이력을 커서 기반으로 조회합니다."
    )
    @GetMapping("/history")
    public ApiResponse<AiMessageHistoryResponse> getHistoryMessages(
            @LoginUser Long userId,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(required = false) Instant cursorCreatedAt
    ) {
        validateHistoryCursor(cursorId, cursorCreatedAt);
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                aiMessageQueryService.getHistoryMessages(userId, cursorId, cursorCreatedAt)
        );
    }

    private void validateHistoryCursor(Long cursorId, Instant cursorCreatedAt) {
        boolean hasCursorId = cursorId != null;
        boolean hasCursorCreatedAt = cursorCreatedAt != null;

        if (hasCursorId != hasCursorCreatedAt) {
            throw new com.bodeum.global.apiPayload.exception.ProjectException(
                    AiErrorCode.AI_INVALID_HISTORY_CURSOR
            );
        }
    }
}
