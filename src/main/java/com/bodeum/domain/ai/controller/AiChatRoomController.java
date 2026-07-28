package com.bodeum.domain.ai.controller;

import com.bodeum.domain.ai.dto.response.AiChatRoomResponse;
import com.bodeum.domain.ai.dto.response.AiGuideConfirmationResponse;
import com.bodeum.domain.ai.dto.request.CreateAiMessageRequest;
import com.bodeum.domain.ai.dto.response.CreateAiMessageResponse;
import com.bodeum.domain.ai.service.AiChatRoomService;
import com.bodeum.domain.ai.service.AiMessageService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

@Tag(name = "AI chat", description = "AI 챗봇 API")
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
            summary = "내 채팅방 조회 또는 생성",
            description = "현재 로그인한 사용자의 AI 채팅방을 조회하고, 존재하지 않으면 새로 생성한다."
    )
    @GetMapping
    public ApiResponse<AiChatRoomResponse> getChatRoom(
            @LoginUser Long userId
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                aiChatRoomService.getOrCreateChatRoom(userId)
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
