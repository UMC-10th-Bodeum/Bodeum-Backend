package com.bodeum.domain.onboarding.service;

import com.bodeum.domain.onboarding.dto.request.ChildProfileCreateReqDTO;
import com.bodeum.domain.onboarding.dto.request.GuardianProfileCreateReqDTO;
import com.bodeum.domain.onboarding.dto.request.InterestRegionCreateReqDTO;
import com.bodeum.domain.onboarding.dto.response.OnboardingStatusResDTO;
import com.bodeum.domain.onboarding.dto.response.OnboardingStepResDTO;
import com.bodeum.domain.user.model.UserAccount;
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
    public OnboardingStepResDTO registerChildProfile(
            Authentication authentication,
            ChildProfileCreateReqDTO request
    ) {
        UserAccount userAccount = userService.getCurrentUser(authentication);
        userAccount.updateChildProfile(
                request.childName(),
                request.birthYear(),
                request.birthMonth(),
                request.careAreas(),
                request.characteristicKeyword()
        );

        return OnboardingStepResDTO.of("CHILD_PROFILE", userAccount.isOnboardingCompleted());
    }

    @Transactional
    public OnboardingStepResDTO registerInterestRegion(
            Authentication authentication,
            InterestRegionCreateReqDTO request
    ) {
        UserAccount userAccount = userService.getCurrentUser(authentication);
        userAccount.updateInterestRegion(request.interests(), request.sido(), request.sigungu());

        return OnboardingStepResDTO.of("INTEREST_REGION", userAccount.isOnboardingCompleted());
    }

    @Transactional
    public OnboardingStepResDTO registerGuardianProfile(
            Authentication authentication,
            GuardianProfileCreateReqDTO request
    ) {
        UserAccount userAccount = userService.getCurrentUser(authentication);
        userAccount.updateGuardianProfile(
                request.guardianNickname(),
                request.guardianType(),
                request.communityRoleType()
        );

        return OnboardingStepResDTO.of("GUARDIAN_PROFILE", userAccount.isOnboardingCompleted());
    }

    @Transactional(readOnly = true)
    public OnboardingStatusResDTO getStatus(Authentication authentication) {
        return OnboardingStatusResDTO.from(userService.getCurrentUser(authentication));
    }
}
