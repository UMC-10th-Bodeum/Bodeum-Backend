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

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final OnboardingService onboardingService;

    @GetMapping("/me/summary")
    public ApiResponse<UserSummaryResponse> getSummary(Authentication authentication) {
        return ApiResponse.of(GeneralSuccessCode.OK, userService.getSummary(authentication));
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getProfile(Authentication authentication) {
        return ApiResponse.of(GeneralSuccessCode.OK, userService.getProfile(authentication));
    }

    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, userService.updateProfile(authentication, request));
    }

    @GetMapping("/me/onboarding-status")
    public ApiResponse<OnboardingStatusResponse> getOnboardingStatus(Authentication authentication) {
        return ApiResponse.of(GeneralSuccessCode.OK, onboardingService.getStatus(authentication));
    }

    @PostMapping("/me/agreements")
    public ApiResponse<UserAgreementResponse> agreeTerms(
            Authentication authentication,
            @Valid @RequestBody CreateUserAgreementRequest request
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, userService.agreeTerms(authentication, request));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(Authentication authentication) {
        userService.withdraw(authentication);

        return ApiResponse.of(GeneralSuccessCode.OK, null);
    }
}
