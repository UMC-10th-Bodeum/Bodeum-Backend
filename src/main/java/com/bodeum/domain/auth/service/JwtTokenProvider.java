package com.bodeum.domain.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;

    public JwtTokenProvider(AuthTokenProperties authTokenProperties) {
        // 시크릿 미설정 시 임시 키로 대체한다. 재시작하면 기존 access token은 전부 무효화된다.
        this.secretKey = StringUtils.hasText(authTokenProperties.getJwtSecret())
                ? Keys.hmacShaKeyFor(authTokenProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8))
                : Jwts.SIG.HS256.key().build();
    }

    public String createAccessToken(Long userId, Instant issuedAt, Instant expiresAt) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 서명·만료를 검증하고 subject의 userId를 반환한다. 유효하지 않으면 empty.
     */
    public Optional<Long> parseUserId(String accessToken) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();

            return Optional.of(Long.parseLong(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
