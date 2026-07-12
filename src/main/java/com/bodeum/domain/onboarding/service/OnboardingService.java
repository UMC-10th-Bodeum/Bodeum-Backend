package com.bodeum.domain.onboarding.service;

import com.bodeum.domain.onboarding.dto.request.CreateChildProfileRequest;
import com.bodeum.domain.onboarding.dto.request.CreateGuardianProfileRequest;
import com.bodeum.domain.onboarding.dto.request.CreateInterestRegionRequest;
import com.bodeum.domain.onboarding.dto.response.OnboardingStatusResponse;
import com.bodeum.domain.onboarding.dto.response.OnboardingStepResponse;
import com.bodeum.domain.onboarding.enumtype.CommunityRoleType;
import com.bodeum.domain.onboarding.enumtype.GuardianType;
import com.bodeum.domain.onboarding.enumtype.OnboardingStep;
import com.bodeum.domain.user.entity.UserAccount;
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
        UserAccount userAccount = userService.getCurrentUser(userId);
        userAccount.updateChildProfile(
                request.childNickname(),
                request.birth(),
                request.disabilityTypeIds(),
                request.keywordText()
        );

        return OnboardingStepResponse.of(OnboardingStep.CHILD_PROFILE, userAccount.isOnboardingCompleted());
    }

    @Transactional
    public OnboardingStepResponse registerInterestRegion(
            Long userId,
            CreateInterestRegionRequest request
    ) {
        UserAccount userAccount = userService.getCurrentUser(userId);
        userAccount.updateInterestRegion(request.interestCategoryIds(), request.regionLevel1(), request.regionLevel2());

        return OnboardingStepResponse.of(OnboardingStep.INTEREST_REGION, userAccount.isOnboardingCompleted());
    }

    @Transactional
    public OnboardingStepResponse registerGuardianProfile(
            Long userId,
            CreateGuardianProfileRequest request
    ) {
        UserAccount userAccount = userService.getCurrentUser(userId);
        userAccount.updateGuardianProfile(
                request.guardianNickname(),
                GuardianType.fromNullable(request.guardianType()),
                CommunityRoleType.fromNullable(request.communityRoleType())
        );

        return OnboardingStepResponse.of(OnboardingStep.GUARDIAN_PROFILE, userAccount.isOnboardingCompleted());
    }

    @Transactional
    public OnboardingStatusResponse skipOnboarding(Long userId) {
        UserAccount userAccount = userService.getCurrentUser(userId);
        userAccount.skipOnboarding();

        return OnboardingStatusResponse.from(userAccount);
    }

    @Transactional(readOnly = true)
    public OnboardingStatusResponse getStatus(Long userId) {
        return OnboardingStatusResponse.from(userService.getCurrentUser(userId));
    }
}
