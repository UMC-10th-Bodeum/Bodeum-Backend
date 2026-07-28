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
        if (!StringUtils.hasText(authTokenProperties.getJwtSecret())) {
            throw new IllegalStateException("bodeum.auth.jwt-secret must be set and must be at least 32 bytes.");
        }

        this.secretKey = Keys.hmacShaKeyFor(authTokenProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String authSubject, Instant issuedAt, Instant expiresAt) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(authSubject)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 서명·만료를 검증하고 subject의 인증 식별자를 반환한다. 유효하지 않으면 empty.
     */
    public Optional<String> parseAuthSubject(String accessToken) {
        return parseClaims(accessToken).map(ParsedClaims::authSubject);
    }

    /**
     * 서명·만료를 검증하고 subject와 발급 시각(issuedAt)을 반환한다. 유효하지 않으면 empty.
     * denylist가 "특정 시각 이전에 발급된 토큰"을 걸러내기 위해 issuedAt을 사용한다.
     */
    public Optional<ParsedClaims> parseClaims(String accessToken) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();

            String subject = claims.getSubject();
            if (!StringUtils.hasText(subject)) {
                return Optional.empty();
            }

            Date issuedAt = claims.getIssuedAt();
            return Optional.of(new ParsedClaims(
                    subject,
                    issuedAt == null ? null : issuedAt.toInstant()
            ));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** 서명 검증을 통과한 access token의 subject(인증 식별자)와 발급 시각. */
    public record ParsedClaims(String authSubject, Instant issuedAt) {
    }
}
