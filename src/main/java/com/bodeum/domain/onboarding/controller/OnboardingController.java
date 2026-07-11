package com.bodeum.domain.onboarding.controller;

import com.bodeum.domain.onboarding.dto.request.CreateChildProfileRequest;
import com.bodeum.domain.onboarding.dto.request.CreateGuardianProfileRequest;
import com.bodeum.domain.onboarding.dto.request.CreateInterestRegionRequest;
import com.bodeum.domain.onboarding.dto.response.OnboardingStatusResponse;
import com.bodeum.domain.onboarding.dto.response.OnboardingStepResponse;
import com.bodeum.domain.onboarding.service.OnboardingService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Onboarding", description = "온보딩 단계별 정보 등록 API")
@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @Operation(summary = "아이 프로필 등록", description = "온보딩 1단계. 아이 이름/생년월/집중 케어 영역/특징 키워드를 등록한다.")
    @PostMapping("/child-profile")
    public ApiResponse<OnboardingStepResponse> registerChildProfile(
            Authentication authentication,
            @Valid @RequestBody CreateChildProfileRequest request
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, onboardingService.registerChildProfile(authentication, request));
    }

    @Operation(summary = "관심사·활동 지역 등록", description = "온보딩 2단계. 가장 큰 관심사(최대 2개)와 주 활동 지역(시/도, 구/군)을 등록한다.")
    @PostMapping("/interest-region")
    public ApiResponse<OnboardingStepResponse> registerInterestRegion(
            Authentication authentication,
            @Valid @RequestBody CreateInterestRegionRequest request
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, onboardingService.registerInterestRegion(authentication, request));
    }

    @Operation(summary = "보호자 프로필 등록", description = "온보딩 3단계. 프로필 닉네임(필수)과 보호자 유형/커뮤니티 성향(선택)을 등록한다.")
    @PostMapping("/guardian-profile")
    public ApiResponse<OnboardingStepResponse> registerGuardianProfile(
            Authentication authentication,
            @Valid @RequestBody CreateGuardianProfileRequest request
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                onboardingService.registerGuardianProfile(authentication, request)
        );
    }

    @Operation(summary = "온보딩 건너뛰기", description = "입력한 정보는 유지한 채 온보딩을 종료하고 홈으로 이동한다.")
    @PostMapping("/skip")
    public ApiResponse<OnboardingStatusResponse> skipOnboarding(Authentication authentication) {
        return ApiResponse.of(GeneralSuccessCode.OK, onboardingService.skipOnboarding(authentication));
    }
}
