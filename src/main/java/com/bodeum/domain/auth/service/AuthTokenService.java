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
import java.util.UUID;
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

        return issueTokens(user, UUID.randomUUID().toString());
    }

    private AuthTokenPair issueTokens(User user, String familyId) {
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
        boolean saved = refreshTokenStore.save(
                tokenHash,
                user.getId(),
                familyId,
                refreshTokenExpiresAt,
                authTokenProperties.getRefreshTokenTtl()
        );
        if (!saved) {
            // 같은 family가 logout과 경쟁해 이미 폐기된 경우 새 토큰을 반환하지 않는다.
            throw new ProjectException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        return new AuthTokenPair(accessToken, refreshToken, accessTokenExpiresAt, refreshTokenExpiresAt);
    }

    @Transactional(readOnly = true)
    public Optional<AuthUserPrincipal> authenticate(String accessToken) {
        return jwtTokenProvider.parseClaims(accessToken)
                .flatMap(this::resolvePrincipal);
    }

    private Optional<AuthUserPrincipal> resolvePrincipal(ParsedClaims claims) {
        // 폐기(로그아웃·탈퇴)된 토큰은 캐시보다 먼저 걸러낸다.
        if (accessTokenDenylist.isRevoked(
                claims.authSubject(),
                claims.tokenId(),
                claims.issuedAt()
        )) {
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
        RefreshTokenStore.ConsumedSession session = refreshTokenStore.consume(tokenHash, Instant.now())
                .orElseThrow(() -> new ProjectException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        User user = userService.findActiveUser(session.userId())
                .orElseThrow(() -> new ProjectException(AuthErrorCode.INACTIVE_USER));

        // refresh 회전 뒤에도 기기별 session-family를 유지한다.
        return issueTokens(user, session.familyId());
    }

    @Transactional
    public void revoke(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenStore.revokeFamily(null, hashToken(refreshToken));
        }
    }

    @Transactional
    public void revoke(Long userId, String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenStore.revokeFamily(userId, hashToken(refreshToken));
        }
    }

    /**
     * 일반 로그아웃에 사용한 access token 하나만 폐기한다.
     * 회원탈퇴의 사용자 단위 폐기는 UserService에서 별도로 처리한다.
     */
    public void revokeAccessToken(String accessToken) {
        ParsedClaims claims = jwtTokenProvider.parseClaims(accessToken)
                .orElseThrow(() -> new ProjectException(AuthErrorCode.INVALID_ACCESS_TOKEN));
        accessTokenDenylist.revokeToken(claims.tokenId(), claims.expiresAt());
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
