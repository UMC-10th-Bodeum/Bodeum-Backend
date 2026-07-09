package com.bodeum.domain.auth.service;

import com.bodeum.domain.auth.model.RefreshTokenSession;
import com.bodeum.domain.auth.repository.RefreshTokenSessionRepository;
import com.bodeum.domain.user.model.UserAccount;
import com.bodeum.domain.user.service.UserAccountStore;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
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

    private static final int TOKEN_BYTE_LENGTH = 32;

    private final UserAccountStore userAccountStore;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthTokenProperties authTokenProperties;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthTokenPair issueTokens(Long userId) {
        purgeExpiredSessions();

        UserAccount userAccount = userAccountStore.findActiveUser(userId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.UNAUTHORIZED));

        Instant now = Instant.now();
        Instant accessTokenExpiresAt = now.plus(authTokenProperties.getAccessTokenTtl());
        Instant refreshTokenExpiresAt = now.plus(authTokenProperties.getRefreshTokenTtl());

        String accessToken = jwtTokenProvider.createAccessToken(userAccount.getAuthSubject(), now, accessTokenExpiresAt);
        String refreshToken = generateRefreshToken();
        refreshTokenSessionRepository.save(RefreshTokenSession.create(
                hashToken(refreshToken),
                userAccount.getId(),
                refreshTokenExpiresAt
        ));

        return new AuthTokenPair(accessToken, refreshToken, accessTokenExpiresAt, refreshTokenExpiresAt);
    }

    @Transactional(readOnly = true)
    public Optional<AuthUserPrincipal> authenticate(String accessToken) {
        return jwtTokenProvider.parseAuthSubject(accessToken)
                .flatMap(userAccountStore::findActiveUserByAuthSubject)
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
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        refreshTokenSessionRepository.delete(session);

        UserAccount userAccount = userAccountStore.findActiveUser(session.getUserId())
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.UNAUTHORIZED));

        return issueTokens(userAccount.getId());
    }

    @Transactional
    public void revoke(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenSessionRepository.findById(hashToken(refreshToken))
                    .ifPresent(refreshTokenSessionRepository::delete);
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
        refreshTokenSessionRepository.deleteExpired(Instant.now());
    }

    private String generateRefreshToken() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
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
