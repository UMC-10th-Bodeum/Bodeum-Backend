package com.bodeum.domain.auth.dto.response;

import com.bodeum.domain.auth.service.AuthTokenService;
import java.time.Instant;

public record AuthTokenResDTO(
        String tokenType,
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt
) {

    public static AuthTokenResDTO from(AuthTokenService.AuthTokenPair tokenPair) {
        return new AuthTokenResDTO(
                "Bearer",
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.accessTokenExpiresAt(),
                tokenPair.refreshTokenExpiresAt()
        );
    }
}
