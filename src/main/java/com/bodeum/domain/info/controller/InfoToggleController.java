package com.bodeum.domain.info.controller;

import com.bodeum.domain.info.dto.response.InfoHelpfulToggleResponse;
import com.bodeum.domain.info.dto.response.InfoScrapToggleResponse;
import com.bodeum.domain.info.service.InfoToggleService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.auth.LoginUser;
import com.bodeum.global.auth.RequireSignupCompleted;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 1. name을 'Info'로 설정하여 기존 Info 섹션 아래로 합칩니다.
@Tag(name = "Info", description = "정보 항목 관련 API")
@SecurityRequirement(name = "JWT TOKEN")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/info-items")
public class InfoToggleController {

    private final InfoToggleService infoToggleService;

    @Operation(
            summary = "정보 스크랩 토글",
            description = "정보 상세 화면 우측 상단의 스크랩 버튼 클릭 시 스크랩을 등록하거나 해제(토글)합니다."
    )
    @RequireSignupCompleted
    @PostMapping("/{infoItemId}/scrap")
    public ApiResponse<InfoScrapToggleResponse> toggleScrap(
            @LoginUser Long userId,
            @Parameter(description = "정보 항목 ID", example = "1")
            @PathVariable Long infoItemId
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                infoToggleService.toggleScrap(userId, infoItemId)
        );
    }

    @Operation(
            summary = "후기 도움돼요 토글",
            description = "정보 후기 리스트의 '도움돼요' 버튼 클릭 시 도움돼요를 등록하거나 취소(토글)합니다."
    )
    @RequireSignupCompleted
    @PostMapping("/{infoItemId}/reviews/{infoReviewId}/helpful")
    public ApiResponse<InfoHelpfulToggleResponse> toggleHelpful(
            @LoginUser Long userId,
            @Parameter(description = "정보 항목 ID", example = "1")
            @PathVariable Long infoItemId,
            @Parameter(description = "정보 후기 ID", example = "10")
            @PathVariable Long infoReviewId
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                infoToggleService.toggleHelpful(userId, infoItemId, infoReviewId)
        );
    }
}