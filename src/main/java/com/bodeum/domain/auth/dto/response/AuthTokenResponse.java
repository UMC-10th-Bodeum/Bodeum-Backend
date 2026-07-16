package com.bodeum.domain.auth.dto.response;

import com.bodeum.domain.auth.service.AuthTokenService;
import java.time.Instant;

public record AuthTokenResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt
) {

    public static AuthTokenResponse from(AuthTokenService.AuthTokenPair tokenPair) {
        return new AuthTokenResponse(
                AuthTokenService.TOKEN_TYPE,
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.accessTokenExpiresAt(),
                tokenPair.refreshTokenExpiresAt()
        );
    }
}
