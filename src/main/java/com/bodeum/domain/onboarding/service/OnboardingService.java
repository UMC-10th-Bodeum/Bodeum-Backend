package com.bodeum.domain.onboarding.service;

import com.bodeum.domain.onboarding.dto.request.CreateChildProfileRequest;
import com.bodeum.domain.onboarding.dto.request.CreateGuardianProfileRequest;
import com.bodeum.domain.onboarding.dto.request.CreateInterestRegionRequest;
import com.bodeum.domain.onboarding.dto.response.OnboardingStatusResponse;
import com.bodeum.domain.onboarding.dto.response.OnboardingStepResponse;
import com.bodeum.domain.onboarding.enumtype.CommunityRoleType;
import com.bodeum.domain.onboarding.enumtype.GuardianType;
import com.bodeum.domain.onboarding.enumtype.OnboardingStep;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserService userService;

    @Transactional
    public OnboardingStepResponse registerChildProfile(
            Long userId,
            CreateChildProfileRequest request
    ) {
        User user = userService.getCurrentUser(userId);
        user.updateChildProfile(
                request.childNickname(),
                request.birth(),
                request.disabilityTypeIds(),
                request.keywordText()
        );

        return OnboardingStepResponse.of(OnboardingStep.CHILD_PROFILE, user.isOnboardingCompleted());
    }

    @Transactional
    public OnboardingStepResponse registerInterestRegion(
            Long userId,
            CreateInterestRegionRequest request
    ) {
        User user = userService.getCurrentUser(userId);
        user.updateInterestRegion(request.interestCategoryIds(), request.regionLevel1(), request.regionLevel2());

        return OnboardingStepResponse.of(OnboardingStep.INTEREST_REGION, user.isOnboardingCompleted());
    }

    @Transactional
    public OnboardingStepResponse registerGuardianProfile(
            Long userId,
            CreateGuardianProfileRequest request
    ) {
        User user = userService.getCurrentUser(userId);
        user.updateGuardianProfile(
                request.guardianNickname(),
                GuardianType.fromNullable(request.guardianType()),
                CommunityRoleType.fromNullable(request.communityRoleType())
        );

        return OnboardingStepResponse.of(OnboardingStep.GUARDIAN_PROFILE, user.isOnboardingCompleted());
    }

    @Transactional
    public OnboardingStatusResponse skipOnboarding(Long userId) {
        User user = userService.getCurrentUser(userId);
        user.skipOnboarding();

        return OnboardingStatusResponse.from(user);
    }

    @Transactional(readOnly = true)
    public OnboardingStatusResponse getStatus(Long userId) {
        return OnboardingStatusResponse.from(userService.getCurrentUser(userId));
    }
}
