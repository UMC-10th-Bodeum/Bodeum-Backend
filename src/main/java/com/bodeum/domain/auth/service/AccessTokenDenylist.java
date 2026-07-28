package com.bodeum.domain.auth.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * stateless access token(JWT)을 만료 전에 강제 무효화하기 위한 denylist.
 * 로그아웃·탈퇴 시 "이 시각 이전에 발급된 이 사용자의 access token은 모두 무효"로 표시한다.
 *
 * <p>키는 유저 단위(authSubject), 값은 폐기 시각(epoch second)이다.
 * redis-enabled=false면 no-op이며, 이 경우 access token은 자연 만료(최대 accessTokenTtl)까지 유효하다.
 *
 * <p>장애 정책: Redis 연결 장애 시 denylist는 <b>fail-open</b>(무효화 미적용)한다. Redis 블립이
 * 전체 인증 장애(503)로 번지는 것을 막고, 노출은 access token 수명(≤accessTokenTtl)으로 한정된다.
 * 더 강한 위협모델이 필요하면 {@link #isRevoked}의 연결 장애 분기를 fail-closed(true 반환)로 바꾼다.
 * 반면 값 자체가 손상된 경우는 데이터 정합성 문제이므로 <b>fail-closed</b>(차단)한다.
 */
@Slf4j
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
        try {
            // TTL을 access token 수명으로 잡으면, 그 시점 이전에 발급된 토큰이 전부 만료된 뒤 자동 정리된다.
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + authSubject,
                    Long.toString(revokedAt.getEpochSecond()),
                    authTokenProperties.getAccessTokenTtl()
            );
        } catch (DataAccessException e) {
            // 등록 실패해도 refresh 토큰 폐기(로그아웃의 주 수단)는 유지되므로 로그아웃 자체는 실패시키지 않는다.
            log.warn("[AUTH] access token denylist 등록 실패 (Redis 장애) authSubject={}", authSubject, e);
        }
    }

    /** 해당 토큰(authSubject·발급시각)이 폐기 대상인지 여부. */
    public boolean isRevoked(String authSubject, Instant tokenIssuedAt) {
        if (!authTokenProperties.isRedisEnabled()) {
            return false;
        }

        String revokedAtRaw;
        try {
            revokedAtRaw = redisTemplate.opsForValue().get(KEY_PREFIX + authSubject);
        } catch (DataAccessException e) {
            // Redis 장애 시 fail-open: 전체 인증 장애를 피한다(노출은 accessTokenTtl로 한정).
            log.warn("[AUTH] denylist 조회 실패 (Redis 장애) - fail-open 처리 authSubject={}", authSubject, e);
            return false;
        }
        if (revokedAtRaw == null) {
            return false;
        }
        if (tokenIssuedAt == null) {
            // 발급 시각을 알 수 없는 토큰은 안전하게 차단한다.
            return true;
        }
        try {
            long revokedAt = Long.parseLong(revokedAtRaw);
            // 동일 초 경계에서 폐기된 토큰이 살아남지 않도록 <= 로 비교한다(fail-closed).
            return tokenIssuedAt.getEpochSecond() <= revokedAt;
        } catch (NumberFormatException e) {
            // 손상된 값은 정합성 문제이므로 fail-closed(차단)하고 키를 제거한다.
            log.warn("[AUTH] denylist 값 손상 - 차단 후 키 삭제 authSubject={} value={}", authSubject, revokedAtRaw);
            try {
                redisTemplate.delete(KEY_PREFIX + authSubject);
            } catch (DataAccessException ignored) {
                // 삭제 실패는 무시(다음 조회에서 재시도).
            }
            return true;
        }
    }
}
