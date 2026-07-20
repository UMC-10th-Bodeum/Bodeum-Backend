package com.bodeum.domain.ai.controller;

import com.bodeum.domain.ai.dto.response.AiChatRoomResponse;
import com.bodeum.domain.ai.dto.response.AiGuideConfirmationResponse;
import com.bodeum.domain.ai.service.AiChatRoomService;
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

@Tag(name = "AI chat", description = "AI 챗봇 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai/chat-room")
public class AiChatRoomController {

    private final AiChatRoomService aiChatRoomService;

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
}
