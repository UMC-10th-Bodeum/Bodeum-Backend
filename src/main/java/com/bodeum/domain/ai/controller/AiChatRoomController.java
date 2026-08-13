package com.bodeum.domain.ai.controller;

import com.bodeum.domain.ai.dto.request.CreateAiMessageRequest;
import com.bodeum.domain.ai.dto.response.AiChatRoomResponse;
import com.bodeum.domain.ai.dto.response.AiGuideConfirmationResponse;
import com.bodeum.domain.ai.dto.response.CreateAiMessageResponse;
import com.bodeum.domain.ai.service.chat.AiChatRoomService;
import com.bodeum.domain.ai.service.chat.AiMessageService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.auth.LoginUser;
import com.bodeum.global.auth.RequireAiTermsAgreed;
import com.bodeum.global.auth.RequireSignupCompleted;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI chat", description = "AI 챗봇 API")
@RequireSignupCompleted
@RequireAiTermsAgreed
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai/chat-room")
public class AiChatRoomController {

    private final AiChatRoomService aiChatRoomService;
    private final AiMessageService aiMessageService;

    @Operation(
            summary = "AI 이용 안내 확인 등록",
            description = "현재 로그인한 사용자가 AI 이용 안내를 확인한 시각을 저장한다."
    )
    @PatchMapping("/guide-confirmation")
    public ApiResponse<AiGuideConfirmationResponse> confirmGuide(
            @LoginUser Long userId
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                aiChatRoomService.confirmGuide(userId)
        );
    }

    @Operation(
            summary = "내 채팅방 조회",
            description = "현재 로그인한 사용자의 AI 채팅방과 화면 진입 상태를 조회합니다."
    )
    @GetMapping
    public ApiResponse<AiChatRoomResponse> getChatRoom(
            @LoginUser Long userId
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                aiChatRoomService.getChatRoom(userId)
        );
    }

    @Operation(
            summary = "내 채팅방 생성",
            description = "현재 로그인한 사용자의 AI 채팅방을 생성합니다."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AiChatRoomResponse> createChatRoom(
            @LoginUser Long userId
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.CREATED,
                aiChatRoomService.createChatRoom(userId)
        );
    }

    @Operation(
            summary = "질문 전송 및 AI 응답 생성",
            description = "질문을 저장하고 RAG 참고자료에 근거한 AI 답변과 출처를 반환합니다."
    )
    @PostMapping("/messages")
    public ApiResponse<CreateAiMessageResponse> createMessage(
            @LoginUser Long userId,
            @Valid @RequestBody CreateAiMessageRequest request
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                aiMessageService.createMessage(userId, request.content().trim())
        );
    }
}
