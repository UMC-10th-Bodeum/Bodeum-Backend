package com.bodeum.domain.ai.controller;

import com.bodeum.domain.ai.dto.request.CreateAiFeedbackRequest;
import com.bodeum.domain.ai.dto.response.CreateAiFeedbackResponse;
import com.bodeum.domain.ai.service.AiFeedbackService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.auth.LoginUser;
import com.bodeum.global.auth.RequireSignupCompleted;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI feedback", description = "AI 메시지 피드백 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai/messages")
public class AiFeedbackController {

    private final AiFeedbackService aiFeedbackService;

    @Operation(
            summary = "AI 메시지 피드백 등록",
            description = "현재 사용자의 AI 메시지에 HELPFUL 또는 INCORRECT 피드백을 등록합니다."
    )
    @RequireSignupCompleted
    @PostMapping("/{aiMessageId}/feedback")
    public ApiResponse<CreateAiFeedbackResponse> createFeedback(
            @LoginUser Long userId,
            @PathVariable Long aiMessageId,
            @Valid @RequestBody CreateAiFeedbackRequest request
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                aiFeedbackService.createFeedback(userId, aiMessageId, request)
        );
    }
}
