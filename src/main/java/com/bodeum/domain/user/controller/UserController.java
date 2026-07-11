package com.bodeum.domain.user.controller;

import com.bodeum.domain.onboarding.dto.response.OnboardingStatusResponse;
import com.bodeum.domain.onboarding.service.OnboardingService;
import com.bodeum.domain.user.dto.request.CreateUserAgreementRequest;
import com.bodeum.domain.user.dto.request.UpdateUserProfileRequest;
import com.bodeum.domain.user.dto.response.UserAgreementResponse;
import com.bodeum.domain.user.dto.response.UserProfileResponse;
import com.bodeum.domain.user.dto.response.UserSummaryResponse;
import com.bodeum.domain.user.service.UserService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 프로필 및 약관 동의 관리 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final OnboardingService onboardingService;

    @Operation(summary = "내 요약 정보 조회", description = "현재 로그인한 사용자의 요약 정보를 조회한다.")
    @GetMapping("/me/summary")
    public ApiResponse<UserSummaryResponse> getSummary(Authentication authentication) {
        return ApiResponse.of(GeneralSuccessCode.OK, userService.getSummary(authentication));
    }

    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 상세 프로필을 조회한다.")
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getProfile(Authentication authentication) {
        return ApiResponse.of(GeneralSuccessCode.OK, userService.getProfile(authentication));
    }

    @Operation(summary = "내 프로필 수정", description = "닉네임, 자녀 정보(이름/생년월/케어 영역/특징 키워드), 관심사, 지역, 보호자 유형, 커뮤니티 성향 등 온보딩에서 입력한 프로필 정보를 수정한다.")
    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, userService.updateProfile(authentication, request));
    }

    @Operation(summary = "온보딩 진행 상태 조회", description = "온보딩 단계별 완료 여부와 다음 이동 화면을 조회한다.")
    @GetMapping("/me/onboarding-status")
    public ApiResponse<OnboardingStatusResponse> getOnboardingStatus(Authentication authentication) {
        return ApiResponse.of(GeneralSuccessCode.OK, onboardingService.getStatus(authentication));
    }

    @Operation(summary = "약관 동의 등록", description = "서비스 이용약관, 개인정보 처리방침, AI 챗봇 이용 동의 여부를 등록한다.")
    @PostMapping("/me/agreements")
    public ApiResponse<UserAgreementResponse> agreeTerms(
            Authentication authentication,
            @Valid @RequestBody CreateUserAgreementRequest request
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, userService.agreeTerms(authentication, request));
    }

    @Operation(summary = "회원 탈퇴", description = "현재 로그인한 사용자를 탈퇴 처리한다.")
    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(Authentication authentication) {
        userService.withdraw(authentication);

        return ApiResponse.of(GeneralSuccessCode.OK, null);
    }
}
