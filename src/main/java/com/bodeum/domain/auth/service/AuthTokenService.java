package com.bodeum.domain.auth.service;

import com.bodeum.domain.auth.entity.RefreshTokenSession;
import com.bodeum.domain.auth.exception.AuthErrorCode;
import com.bodeum.domain.auth.repository.RefreshTokenSessionRepository;
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
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;

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
        refreshTokenSessionRepository.save(RefreshTokenSession.create(
                hashToken(refreshToken),
                user.getId(),
                refreshTokenExpiresAt
        ));

        return new AuthTokenPair(accessToken, refreshToken, accessTokenExpiresAt, refreshTokenExpiresAt);
    }

    @Transactional(readOnly = true)
    public Optional<AuthUserPrincipal> authenticate(String accessToken) {
        return jwtTokenProvider.parseAuthSubject(accessToken)
                .flatMap(userService::findActiveUserByAuthSubject)
                .map(this::toPrincipal);
    }

    @Transactional
    public AuthTokenPair refresh(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        Instant now = Instant.now();

        RefreshTokenSession session = refreshTokenSessionRepository.findByTokenHashForUpdate(tokenHash)
                .orElse(null);
        if (session == null || session.isExpired(now)) {
            if (session != null) {
                refreshTokenSessionRepository.delete(session);
            }
            throw new ProjectException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        refreshTokenSessionRepository.delete(session);

        // issueTokens가 활성 사용자 검증(INACTIVE_USER)을 수행하므로 여기서 중복 조회하지 않는다.
        return issueTokens(session.getUserId());
    }

    @Transactional
    public void revoke(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenSessionRepository.findById(hashToken(refreshToken))
                    .ifPresent(refreshTokenSessionRepository::delete);
        }
    }

    @Transactional
    public void revoke(Long userId, String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenSessionRepository.findById(hashToken(refreshToken))
                    .filter(session -> session.getUserId().equals(userId))
                    .ifPresent(refreshTokenSessionRepository::delete);
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
        refreshTokenSessionRepository.deleteExpired(Instant.now());
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
