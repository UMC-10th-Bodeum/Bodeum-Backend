package com.bodeum.domain.auth.service;

import com.bodeum.domain.user.model.UserAccount;
import com.bodeum.domain.user.service.UserAccountStore;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import com.bodeum.global.auth.AuthUserPrincipal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * access token은 stateless JWT, refresh token은 서버 저장(회전·폐기 가능) 방식.
 */
@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private static final int TOKEN_BYTE_LENGTH = 32;

    private final UserAccountStore userAccountStore;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthTokenProperties authTokenProperties;

    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentMap<String, RefreshTokenSession> refreshTokenSessions = new ConcurrentHashMap<>();

    public AuthTokenPair issueTokens(Long userId) {
        purgeExpiredSessions();

        Instant now = Instant.now();
        Instant accessTokenExpiresAt = now.plus(authTokenProperties.getAccessTokenTtl());
        Instant refreshTokenExpiresAt = now.plus(authTokenProperties.getRefreshTokenTtl());

        String accessToken = jwtTokenProvider.createAccessToken(userId, now, accessTokenExpiresAt);
        String refreshToken = generateRefreshToken();
        refreshTokenSessions.put(refreshToken, new RefreshTokenSession(userId, refreshTokenExpiresAt));

        return new AuthTokenPair(accessToken, refreshToken, accessTokenExpiresAt, refreshTokenExpiresAt);
    }

    public Optional<AuthUserPrincipal> authenticate(String accessToken) {
        return jwtTokenProvider.parseUserId(accessToken)
                .flatMap(userAccountStore::findActiveUser)
                .map(this::toPrincipal);
    }

    public AuthTokenPair refresh(String refreshToken) {
        RefreshTokenSession session = refreshTokenSessions.remove(refreshToken);
        if (session == null || session.isExpired()) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        UserAccount userAccount = userAccountStore.findActiveUser(session.userId())
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.UNAUTHORIZED));

        return issueTokens(userAccount.getId());
    }

    public void revoke(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenSessions.remove(refreshToken);
        }
    }

    private AuthUserPrincipal toPrincipal(UserAccount userAccount) {
        return new AuthUserPrincipal(
                userAccount.getId(),
                userAccount.getProvider(),
                userAccount.getNickname(),
                userAccount.getEmail()
        );
    }

    private void purgeExpiredSessions() {
        refreshTokenSessions.values().removeIf(RefreshTokenSession::isExpired);
    }

    private String generateRefreshToken() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public record AuthTokenPair(
            String accessToken,
            String refreshToken,
            Instant accessTokenExpiresAt,
            Instant refreshTokenExpiresAt
    ) {
    }

    private record RefreshTokenSession(Long userId, Instant expiresAt) {

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
