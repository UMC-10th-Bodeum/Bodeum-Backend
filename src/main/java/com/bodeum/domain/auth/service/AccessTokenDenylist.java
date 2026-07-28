package com.bodeum.domain.auth.service;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * stateless access token(JWT)을 만료 전에 강제 무효화하기 위한 denylist.
 * 일반 로그아웃은 현재 token(jti)만, 회원탈퇴는 사용자 단위 cutoff로 모든 token을 폐기한다.
 *
 * <p>사용자 키 값은 폐기 시각(epoch second), 토큰 키 값은 단순 marker다.
 * redis-enabled=false면 no-op이며, 이 경우 access token은 자연 만료(최대 accessTokenTtl)까지 유효하다.
 *
 * <p>장애 정책은 읽기/쓰기를 분리한다.
 * <ul>
 *   <li><b>쓰기({@link #revokeToken}, {@link #revokeAllBefore})는 strict</b>: Redis 장애 시 예외를 전파한다.
 *       로그아웃·탈퇴 같은
 *       보안 무효화 실패를 삼키면 "폐기됐다고 응답했는데 실제로는 폐기 안 된" 상태가 되어 위험하다.
 *       호출자는 이 실패를 트랜잭션 롤백 등으로 처리해 정합성을 지켜야 한다.</li>
 *   <li><b>읽기({@link #isRevoked})는 fail-open</b>: Redis 블립이 전체 인증 장애(503)로 번지지 않게 하고,
 *       노출은 access token 수명(≤accessTokenTtl)으로 한정된다. 다만 값이 손상된 경우는 정합성 문제이므로
 *       fail-closed(차단)한다.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessTokenDenylist {

    private static final String KEY_PREFIX = "bodeum:auth:denylist:";
    private static final String TOKEN_KEY_PREFIX = "bodeum:auth:denylist-token:";

    private final AuthTokenProperties authTokenProperties;
    private final StringRedisTemplate redisTemplate;

    /**
     * authSubject의 access token 중 revokedAt 이전에 발급된 것을 모두 무효화한다.
     * strict: Redis 장애 시 예외를 전파하므로, 호출자는 이 실패가 삼켜지지 않도록 처리해야 한다.
     */
    public void revokeAllBefore(String authSubject, Instant revokedAt) {
        if (!authTokenProperties.isRedisEnabled() || authSubject == null) {
            return;
        }
        // TTL을 access token 수명으로 잡으면, 그 시점 이전에 발급된 토큰이 전부 만료된 뒤 자동 정리된다.
        // 실패(DataAccessException)는 의도적으로 전파한다.
        redisTemplate.opsForValue().set(
                KEY_PREFIX + authSubject,
                Long.toString(revokedAt.getEpochSecond()),
                authTokenProperties.getAccessTokenTtl()
        );
    }

    /**
     * tokenId(jti) 하나만 만료 시각까지 폐기한다.
     * strict: Redis 장애 시 예외를 전파한다.
     */
    public void revokeToken(String tokenId, Instant expiresAt) {
        if (!authTokenProperties.isRedisEnabled() || tokenId == null || expiresAt == null) {
            return;
        }

        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + tokenId, "1", ttl);
    }

    /** 해당 토큰(jti 또는 authSubject·발급시각)이 폐기 대상인지 여부. */
    public boolean isRevoked(String authSubject, String tokenId, Instant tokenIssuedAt) {
        if (!authTokenProperties.isRedisEnabled()) {
            return false;
        }

        try {
            if (tokenId == null || Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_KEY_PREFIX + tokenId))) {
                return true;
            }
        } catch (DataAccessException e) {
            // Redis 장애 시 fail-open: 전체 인증 장애를 피한다(노출은 accessTokenTtl로 한정).
            log.warn("[AUTH] token denylist 조회 실패 (Redis 장애) - fail-open 처리 tokenId={}", tokenId, e);
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

    /** 사용자 단위 cutoff만 확인하는 기존 호출용 편의 메서드. */
    public boolean isRevoked(String authSubject, Instant tokenIssuedAt) {
        return isRevoked(authSubject, "", tokenIssuedAt);
    }
}
