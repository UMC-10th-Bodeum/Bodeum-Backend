package com.bodeum.domain.auth.dto.response;

import com.bodeum.domain.auth.enumtype.AuthNextStep;
import com.bodeum.domain.auth.service.AuthTokenService;
import com.bodeum.domain.user.entity.UserAccount;
import java.time.Instant;

public record AuthLoginResponse(
        Long userId,
        String provider,
        String nickname,
        String tokenType,
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt,
        boolean isNewUser,
        boolean agreementCompleted,
        boolean onboardingCompleted,
        AuthNextStep nextStep
) {

    public static AuthLoginResponse of(
            UserAccount userAccount,
            AuthTokenService.AuthTokenPair tokenPair,
            boolean isNewUser,
            AuthNextStep nextStep
    ) {
        return new AuthLoginResponse(
                userAccount.getId(),
                userAccount.getProvider().getPath(),
                userAccount.getNickname(),
                AuthTokenService.TOKEN_TYPE,
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.accessTokenExpiresAt(),
                tokenPair.refreshTokenExpiresAt(),
                isNewUser,
                userAccount.isAgreementCompleted(),
                userAccount.isOnboardingCompleted(),
                nextStep
        );
    }
}
