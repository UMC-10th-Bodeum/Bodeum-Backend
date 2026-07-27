package com.bodeum.domain.ai.controller;

import com.bodeum.domain.ai.dto.response.AiChatStarterResponse;
import com.bodeum.domain.ai.service.AiChatStarterService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.auth.LoginUser;
import com.bodeum.global.auth.RequireSignupCompleted;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI chat starter", description = "AI 챗봇 초기 대화 콘텐츠 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai/chat-room/starter")
public class AiChatStarterController {

    private final AiChatStarterService aiChatStarterService;

    @Operation(
            summary = "초기 대화 콘텐츠 조회",
            description = "AI 챗봇 초기 진입 시 노출할 인사말과 고정 추천 질문 5개를 조회합니다."
    )
    @RequireSignupCompleted
    @GetMapping
    public ApiResponse<AiChatStarterResponse> getChatStarter(
            @LoginUser Long userId
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                aiChatStarterService.getChatStarter(userId)
        );
    }
}
