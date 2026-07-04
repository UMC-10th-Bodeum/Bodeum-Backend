package com.bodeum.domain.onboarding.controller;

import com.bodeum.domain.onboarding.dto.request.ChildProfileCreateReqDTO;
import com.bodeum.domain.onboarding.dto.request.GuardianProfileCreateReqDTO;
import com.bodeum.domain.onboarding.dto.request.InterestRegionCreateReqDTO;
import com.bodeum.domain.onboarding.dto.response.OnboardingStepResDTO;
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
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping("/child-profile")
    public ApiResponse<OnboardingStepResDTO> registerChildProfile(
            Authentication authentication,
            @Valid @RequestBody ChildProfileCreateReqDTO request
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, onboardingService.registerChildProfile(authentication, request));
    }

    @PostMapping("/interest-region")
    public ApiResponse<OnboardingStepResDTO> registerInterestRegion(
            Authentication authentication,
            @Valid @RequestBody InterestRegionCreateReqDTO request
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, onboardingService.registerInterestRegion(authentication, request));
    }

    @PostMapping("/guardian-profile")
    public ApiResponse<OnboardingStepResDTO> registerGuardianProfile(
            Authentication authentication,
            @Valid @RequestBody GuardianProfileCreateReqDTO request
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, onboardingService.registerGuardianProfile(authentication, request));
    }
}
