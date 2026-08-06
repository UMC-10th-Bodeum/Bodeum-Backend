package com.bodeum.domain.onboarding.controller;

import com.bodeum.domain.onboarding.dto.request.CreateChildProfileRequest;
import com.bodeum.domain.onboarding.dto.request.CreateGuardianProfileRequest;
import com.bodeum.domain.onboarding.dto.request.CreateInterestRegionRequest;
import com.bodeum.domain.onboarding.dto.response.OnboardingStatusResponse;
import com.bodeum.domain.onboarding.dto.response.OnboardingStepResponse;
import com.bodeum.domain.onboarding.service.OnboardingService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Onboarding", description = "온보딩 단계별 정보 등록 API")
@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @Operation(summary = "아이 프로필 등록", description = "온보딩 1단계. 아이 이름/생년월/집중 케어 영역/특징 키워드를 등록한다.")
    @PostMapping("/child-profile")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OnboardingStepResponse> registerChildProfile(
            @LoginUser Long userId,
            @Valid @RequestBody CreateChildProfileRequest request
    ) {
        return ApiResponse.of(GeneralSuccessCode.CREATED, onboardingService.registerChildProfile(userId, request));
    }

    @Operation(summary = "관심사·활동 지역 등록", description = "온보딩 2단계. 가장 큰 관심사(최대 2개)와 주 활동 지역(시/도, 구/군)을 등록한다.")
    @PostMapping("/interest-region")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OnboardingStepResponse> registerInterestRegion(
            @LoginUser Long userId,
            @Valid @RequestBody CreateInterestRegionRequest request
    ) {
        return ApiResponse.of(GeneralSuccessCode.CREATED, onboardingService.registerInterestRegion(userId, request));
    }

    @Operation(summary = "보호자 프로필 등록", description = "온보딩 3단계. 프로필 닉네임(필수)과 보호자 유형/커뮤니티 성향(선택)을 등록한다.")
    @PostMapping("/guardian-profile")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OnboardingStepResponse> registerGuardianProfile(
            @LoginUser Long userId,
            @Valid @RequestBody CreateGuardianProfileRequest request
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.CREATED,
                onboardingService.registerGuardianProfile(userId, request)
        );
    }

    @Operation(summary = "온보딩 건너뛰기", description = "입력한 정보는 유지한 채 온보딩을 종료하고 홈으로 이동한다.")
    @PostMapping("/skip")
    public ApiResponse<OnboardingStatusResponse> skipOnboarding(@LoginUser Long userId) {
        return ApiResponse.of(GeneralSuccessCode.OK, onboardingService.skipOnboarding(userId));
    }

    @Operation(
            summary = "온보딩 그만두기",
            description = "입력한 정보를 모두 삭제하고 온보딩을 종료한다. 정식 회원으로 가입 처리되며 홈으로 이동한다."
    )
    @PostMapping("/quit")
    public ApiResponse<OnboardingStatusResponse> quitOnboarding(@LoginUser Long userId) {
        return ApiResponse.of(GeneralSuccessCode.OK, onboardingService.quitOnboarding(userId));
    }
}
