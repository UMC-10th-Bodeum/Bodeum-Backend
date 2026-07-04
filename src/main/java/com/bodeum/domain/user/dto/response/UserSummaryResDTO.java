package com.bodeum.domain.user.dto.response;

import com.bodeum.domain.auth.enumtype.SocialProvider;
import com.bodeum.domain.user.model.UserAccount;

public record UserSummaryResDTO(
        Long userId,
        String nickname,
        String email,
        SocialProvider provider,
        boolean agreementCompleted,
        boolean onboardingCompleted
) {

    public static UserSummaryResDTO from(UserAccount userAccount) {
        return new UserSummaryResDTO(
                userAccount.getId(),
                userAccount.getNickname(),
                userAccount.getEmail(),
                userAccount.getProvider(),
                userAccount.isAgreementCompleted(),
                userAccount.isOnboardingCompleted()
        );
    }
}
