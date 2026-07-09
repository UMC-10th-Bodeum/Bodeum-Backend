package com.bodeum.domain.onboarding.service;

import com.bodeum.domain.onboarding.dto.request.CreateChildProfileRequest;
import com.bodeum.domain.onboarding.dto.request.CreateGuardianProfileRequest;
import com.bodeum.domain.onboarding.dto.request.CreateInterestRegionRequest;
import com.bodeum.domain.onboarding.dto.response.OnboardingStatusResponse;
import com.bodeum.domain.onboarding.dto.response.OnboardingStepResponse;
import com.bodeum.domain.onboarding.enumtype.OnboardingStep;
import com.bodeum.domain.user.entity.UserAccount;
import com.bodeum.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserService userService;

    @Transactional
    public OnboardingStepResponse registerChildProfile(
            Authentication authentication,
            CreateChildProfileRequest request
    ) {
        UserAccount userAccount = userService.getCurrentUser(authentication);
        userAccount.updateChildProfile(
                request.childName(),
                request.birthYear(),
                request.birthMonth(),
                request.careAreas(),
                request.characteristicKeyword()
        );

        return OnboardingStepResponse.of(OnboardingStep.CHILD_PROFILE, userAccount.isOnboardingCompleted());
    }

    @Transactional
    public OnboardingStepResponse registerInterestRegion(
            Authentication authentication,
            CreateInterestRegionRequest request
    ) {
        UserAccount userAccount = userService.getCurrentUser(authentication);
        userAccount.updateInterestRegion(request.interests(), request.sido(), request.sigungu());

        return OnboardingStepResponse.of(OnboardingStep.INTEREST_REGION, userAccount.isOnboardingCompleted());
    }

    @Transactional
    public OnboardingStepResponse registerGuardianProfile(
            Authentication authentication,
            CreateGuardianProfileRequest request
    ) {
        UserAccount userAccount = userService.getCurrentUser(authentication);
        userAccount.updateGuardianProfile(
                request.guardianNickname(),
                request.guardianType(),
                request.communityRoleType()
        );

        return OnboardingStepResponse.of(OnboardingStep.GUARDIAN_PROFILE, userAccount.isOnboardingCompleted());
    }

    @Transactional(readOnly = true)
    public OnboardingStatusResponse getStatus(Authentication authentication) {
        return OnboardingStatusResponse.from(userService.getCurrentUser(authentication));
    }
}
