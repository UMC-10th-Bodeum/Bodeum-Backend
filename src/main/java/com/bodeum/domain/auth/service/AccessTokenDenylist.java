package com.bodeum.domain.auth.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * stateless access token(JWT)을 만료 전에 강제 무효화하기 위한 denylist.
 * 로그아웃·탈퇴 시 "이 시각 이전에 발급된 이 사용자의 access token은 모두 무효"로 표시한다.
 *
 * <p>키는 유저 단위(authSubject), 값은 폐기 시각(epoch second)이다.
 * redis-enabled=false면 no-op이며, 이 경우 access token은 자연 만료(최대 accessTokenTtl)까지 유효하다.
 */
@Component
@RequiredArgsConstructor
public class AccessTokenDenylist {

    private static final String KEY_PREFIX = "bodeum:auth:denylist:";

    private final AuthTokenProperties authTokenProperties;
    private final StringRedisTemplate redisTemplate;

    /** authSubject의 access token 중 revokedAt 이전에 발급된 것을 모두 무효화한다. */
    public void revokeAllBefore(String authSubject, Instant revokedAt) {
        if (!authTokenProperties.isRedisEnabled() || authSubject == null) {
            return;
        }
        // TTL을 access token 수명으로 잡으면, 그 시점 이전에 발급된 토큰이 전부 만료된 뒤 자동 정리된다.
        redisTemplate.opsForValue().set(
                KEY_PREFIX + authSubject,
                Long.toString(revokedAt.getEpochSecond()),
                authTokenProperties.getAccessTokenTtl()
        );
    }

    /** 해당 토큰(authSubject·발급시각)이 폐기 대상인지 여부. */
    public boolean isRevoked(String authSubject, Instant tokenIssuedAt) {
        if (!authTokenProperties.isRedisEnabled()) {
            return false;
        }

        String revokedAtRaw = redisTemplate.opsForValue().get(KEY_PREFIX + authSubject);
        if (revokedAtRaw == null) {
            return false;
        }
        if (tokenIssuedAt == null) {
            // 발급 시각을 알 수 없는 토큰은 안전하게 차단한다.
            return true;
        }
        try {
            long revokedAt = Long.parseLong(revokedAtRaw);
            return tokenIssuedAt.getEpochSecond() < revokedAt;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
