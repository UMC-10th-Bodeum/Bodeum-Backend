package com.bodeum.domain.ai.controller;

import com.bodeum.domain.ai.dto.response.AiChatStarterResponse;
import com.bodeum.domain.ai.service.chat.AiChatStarterService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.auth.LoginUser;
import com.bodeum.global.auth.RequireAiTermsAgreed;
import com.bodeum.global.auth.RequireSignupCompleted;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI chat starter", description = "AI 챗봇 초기 대화 콘텐츠 API")
@RequireSignupCompleted
@RequireAiTermsAgreed
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai/chat-room/starter")
public class AiChatStarterController {

    private final AiChatStarterService aiChatStarterService;

    @Operation(
            summary = "초기 대화 콘텐츠 생성",
            description = "AI 챗봇 초기 인사말을 저장하고 추천 질문 5개와 함께 반환합니다."
    )
    @PostMapping
    public ApiResponse<AiChatStarterResponse> createChatStarter(
            @LoginUser Long userId
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                aiChatStarterService.getChatStarter(userId)
        );
    }
}
