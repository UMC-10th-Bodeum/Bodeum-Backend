package com.bodeum.domain.user.dto.response;

import com.bodeum.domain.user.entity.UserAccount;

public record UserSummaryResponse(
        Long userId,
        String nickname,
        String email,
        String provider,
        String profileImageUrl,
        int point,
        int level,
        String badgeName,
        boolean agreementCompleted,
        boolean onboardingCompleted
) {

    public static UserSummaryResponse from(UserAccount userAccount) {
        return new UserSummaryResponse(
                userAccount.getId(),
                userAccount.getNickname(),
                userAccount.getEmail(),
                userAccount.getProvider().getPath(),
                userAccount.getProfileImageUrl(),
                userAccount.getPoint(),
                userAccount.getGuardianLevel().getLevelNumber(),
                userAccount.getGuardianLevel().getBadgeName(),
                userAccount.isAgreementCompleted(),
                userAccount.isOnboardingCompleted()
        );
    }
}
