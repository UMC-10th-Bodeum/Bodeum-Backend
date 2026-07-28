package com.bodeum.domain.onboarding.service;

import com.bodeum.domain.onboarding.dto.request.CreateChildProfileRequest;
import com.bodeum.domain.onboarding.dto.request.CreateGuardianProfileRequest;
import com.bodeum.domain.onboarding.dto.request.CreateInterestRegionRequest;
import com.bodeum.domain.onboarding.dto.response.OnboardingStatusResponse;
import com.bodeum.domain.onboarding.dto.response.OnboardingStepResponse;
import com.bodeum.domain.onboarding.enums.OnboardingStep;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.region.service.RegionService;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserService userService;
    private final RegionService regionService;

    @Transactional
    public OnboardingStepResponse registerChildProfile(
            Long userId,
            CreateChildProfileRequest request
    ) {
        User user = userService.getCurrentUser(userId);
        user.updateChildProfile(
                request.childNickname(),
                request.birth(),
                request.disabilityTypes(),
                request.keywordText()
        );
        user.markRegisteredIfResolved();

        return OnboardingStepResponse.of(OnboardingStep.CHILD_PROFILE, user.isOnboardingCompleted());
    }

    @Transactional
    public OnboardingStepResponse registerInterestRegion(
            Long userId,
            CreateInterestRegionRequest request
    ) {
        User user = userService.getCurrentUser(userId);
        Region region = regionService.getById(request.regionId());
        user.updateInterestRegion(request.interestCategories(), region);
        user.markRegisteredIfResolved();

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
                request.guardianType(),
                request.communityRoleType()
        );
        user.markRegisteredIfResolved();

        return OnboardingStepResponse.of(OnboardingStep.GUARDIAN_PROFILE, user.isOnboardingCompleted());
    }

    @Transactional
    public OnboardingStatusResponse skipOnboarding(Long userId) {
        User user = userService.getCurrentUser(userId);
        user.skipOnboarding();
        user.markRegisteredIfResolved();

        return OnboardingStatusResponse.from(user);
    }

    @Transactional
    public OnboardingStatusResponse quitOnboarding(Long userId) {
        User user = userService.getCurrentUser(userId);
        user.quitOnboarding();
        user.markRegisteredIfResolved();

        return OnboardingStatusResponse.from(user);
    }

    @Transactional(readOnly = true)
    public OnboardingStatusResponse getStatus(Long userId) {
        return OnboardingStatusResponse.from(userService.getCurrentUser(userId));
    }
}
