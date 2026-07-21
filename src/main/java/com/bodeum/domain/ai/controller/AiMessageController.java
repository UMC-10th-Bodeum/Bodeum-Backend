package com.bodeum.domain.ai.controller;

import com.bodeum.domain.ai.dto.response.AiTodayMessageResponse;
import com.bodeum.domain.ai.service.AiMessageQueryService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
