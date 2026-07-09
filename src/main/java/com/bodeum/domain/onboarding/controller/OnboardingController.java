package com.bodeum.domain.onboarding.controller;

import com.bodeum.domain.onboarding.dto.request.CreateChildProfileRequest;
import com.bodeum.domain.onboarding.dto.request.CreateGuardianProfileRequest;
import com.bodeum.domain.onboarding.dto.request.CreateInterestRegionRequest;
import com.bodeum.domain.onboarding.dto.response.OnboardingStepResponse;
import com.bodeum.domain.onboarding.service.OnboardingService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping("/child-profile")
    public ApiResponse<OnboardingStepResponse> registerChildProfile(
            Authentication authentication,
            @Valid @RequestBody CreateChildProfileRequest request
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, onboardingService.registerChildProfile(authentication, request));
    }

    @PostMapping("/interest-region")
    public ApiResponse<OnboardingStepResponse> registerInterestRegion(
            Authentication authentication,
            @Valid @RequestBody CreateInterestRegionRequest request
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, onboardingService.registerInterestRegion(authentication, request));
    }

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
}
