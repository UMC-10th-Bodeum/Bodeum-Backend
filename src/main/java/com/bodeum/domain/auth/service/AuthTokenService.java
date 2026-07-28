package com.bodeum.domain.auth.service;

import com.bodeum.domain.auth.exception.AuthErrorCode;
import com.bodeum.domain.auth.service.JwtTokenProvider.ParsedClaims;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.service.UserService;
import com.bodeum.global.apiPayload.exception.ProjectException;
import com.bodeum.global.auth.AuthUserPrincipal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * access token은 stateless JWT, refresh token은 서버 저장(회전·폐기 가능) 방식.
 */
@Service
@RequiredArgsConstructor
public class AuthTokenService {

    public static final String TOKEN_TYPE = "Bearer";
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthTokenProperties authTokenProperties;
    private final RefreshTokenStore refreshTokenStore;
    private final AuthPrincipalCache authPrincipalCache;
    private final AccessTokenDenylist accessTokenDenylist;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthTokenPair issueTokens(Long userId) {
        purgeExpiredSessions();

        User user = userService.findActiveUser(userId)
                .orElseThrow(() -> new ProjectException(AuthErrorCode.INACTIVE_USER));

        Instant now = Instant.now();
        Instant accessTokenExpiresAt = now.plus(authTokenProperties.getAccessTokenTtl());
        Instant refreshTokenExpiresAt = now.plus(authTokenProperties.getRefreshTokenTtl());

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getAuthSubject(),
                now,
                accessTokenExpiresAt
        );
        String refreshToken = generateRefreshToken();
        String tokenHash = hashToken(refreshToken);
        refreshTokenStore.save(
                tokenHash,
                user.getId(),
                refreshTokenExpiresAt,
                authTokenProperties.getRefreshTokenTtl()
        );

        return new AuthTokenPair(accessToken, refreshToken, accessTokenExpiresAt, refreshTokenExpiresAt);
    }

    @Transactional(readOnly = true)
    public Optional<AuthUserPrincipal> authenticate(String accessToken) {
        return jwtTokenProvider.parseClaims(accessToken)
                .flatMap(this::resolvePrincipal);
    }

    private Optional<AuthUserPrincipal> resolvePrincipal(ParsedClaims claims) {
        // 폐기(로그아웃·탈퇴)된 토큰은 캐시보다 먼저 걸러낸다.
        if (accessTokenDenylist.isRevoked(claims.authSubject(), claims.issuedAt())) {
            return Optional.empty();
        }

        Optional<AuthUserPrincipal> cached = authPrincipalCache.get(claims.authSubject());
        if (cached.isPresent()) {
            return cached;
        }

        Optional<AuthUserPrincipal> principal = userService
                .findActiveUserByAuthSubject(claims.authSubject())
                .map(this::toPrincipal);
        principal.ifPresent(p -> authPrincipalCache.put(claims.authSubject(), p));
        return principal;
    }

    @Transactional
    public AuthTokenPair refresh(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        Long userId = refreshTokenStore.consumeUserId(tokenHash, Instant.now())
                .orElseThrow(() -> new ProjectException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        // issueTokens가 활성 사용자 검증(INACTIVE_USER)을 수행하므로 여기서 중복 조회하지 않는다.
        return issueTokens(userId);
    }

    @Transactional
    public void revoke(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenStore.revoke(hashToken(refreshToken));
        }
    }

    @Transactional
    public void revoke(Long userId, String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenStore.revoke(userId, hashToken(refreshToken));
        }
    }

    private AuthUserPrincipal toPrincipal(User user) {
        return new AuthUserPrincipal(
                user.getId(),
                user.getProvider(),
                user.getNickname(),
                user.getEmail()
        );
    }

    private void purgeExpiredSessions() {
        refreshTokenStore.purgeExpired(Instant.now());
    }

    private String generateRefreshToken() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ProjectException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is not available.", e);
        }
    }

    public record AuthTokenPair(
            String accessToken,
            String refreshToken,
            Instant accessTokenExpiresAt,
            Instant refreshTokenExpiresAt
    ) {
    }

}
