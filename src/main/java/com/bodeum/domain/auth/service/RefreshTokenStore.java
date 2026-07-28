package com.bodeum.domain.auth.service;

import com.bodeum.domain.auth.entity.RefreshTokenSession;
import com.bodeum.domain.auth.repository.RefreshTokenSessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * refresh 토큰 세션 저장소. redis-enabled면 Redis(키별 TTL), 아니면 DB(JPA)에 저장한다.
 *
 * <p>Redis 경로에서는 refresh 키(refresh:{hash} = userId)와 함께
 * 유저별 세션 역인덱스(user-sessions:{userId} = {hash...})를 유지해,
 * 탈퇴 시 한 사용자의 모든 기기 세션을 한 번에 폐기할 수 있게 한다.
 *
 * <p>refresh 토큰과 로직을 AuthTokenService에서 분리한 이유는, 탈퇴 처리를 하는
 * UserService도 세션 폐기가 필요한데 AuthTokenService가 UserService에 의존해
 * 순환참조가 되기 때문이다. 양쪽이 이 컴포넌트에만 의존하도록 한다.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String REFRESH_KEY_PREFIX = "bodeum:auth:refresh:";
    private static final String SESSIONS_KEY_PREFIX = "bodeum:auth:user-sessions:";

    private final AuthTokenProperties authTokenProperties;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final StringRedisTemplate redisTemplate;

    public void save(String tokenHash, Long userId, Instant expiresAt, Duration ttl) {
        if (authTokenProperties.isRedisEnabled()) {
            redisTemplate.opsForValue().set(REFRESH_KEY_PREFIX + tokenHash, userId.toString(), ttl);
            String sessionsKey = SESSIONS_KEY_PREFIX + userId;
            redisTemplate.opsForSet().add(sessionsKey, tokenHash);
            // 역인덱스 Set은 멤버별 TTL이 없으므로, 최신 세션 만료 시점까지로 Set 수명을 갱신한다.
            redisTemplate.expire(sessionsKey, ttl);
            return;
        }
        refreshTokenSessionRepository.save(RefreshTokenSession.create(tokenHash, userId, expiresAt));
    }

    /**
     * refresh 토큰을 1회 소비(삭제)하고 소유자 userId를 반환한다.
     * 없거나 만료됐으면 empty(호출자가 INVALID_REFRESH_TOKEN 처리).
     */
    public Optional<Long> consumeUserId(String tokenHash, Instant now) {
        if (authTokenProperties.isRedisEnabled()) {
            String storedUserId = redisTemplate.opsForValue().getAndDelete(REFRESH_KEY_PREFIX + tokenHash);
            if (storedUserId == null) {
                return Optional.empty();
            }
            Optional<Long> userId = parseUserId(storedUserId);
            userId.ifPresent(id -> redisTemplate.opsForSet().remove(SESSIONS_KEY_PREFIX + id, tokenHash));
            return userId;
        }

        RefreshTokenSession session = refreshTokenSessionRepository.findByTokenHashForUpdate(tokenHash)
                .orElse(null);
        if (session == null || session.isExpired(now)) {
            if (session != null) {
                refreshTokenSessionRepository.delete(session);
            }
            return Optional.empty();
        }
        refreshTokenSessionRepository.delete(session);
        return Optional.of(session.getUserId());
    }

    public void revoke(String tokenHash) {
        if (authTokenProperties.isRedisEnabled()) {
            String storedUserId = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + tokenHash);
            redisTemplate.delete(REFRESH_KEY_PREFIX + tokenHash);
            parseUserId(storedUserId)
                    .ifPresent(id -> redisTemplate.opsForSet().remove(SESSIONS_KEY_PREFIX + id, tokenHash));
            return;
        }
        refreshTokenSessionRepository.findById(tokenHash)
                .ifPresent(refreshTokenSessionRepository::delete);
    }

    public void revoke(Long userId, String tokenHash) {
        if (authTokenProperties.isRedisEnabled()) {
            String storedUserId = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + tokenHash);
            if (userId.toString().equals(storedUserId)) {
                redisTemplate.delete(REFRESH_KEY_PREFIX + tokenHash);
                redisTemplate.opsForSet().remove(SESSIONS_KEY_PREFIX + userId, tokenHash);
            }
            return;
        }
        refreshTokenSessionRepository.findById(tokenHash)
                .filter(session -> session.getUserId().equals(userId))
                .ifPresent(refreshTokenSessionRepository::delete);
    }

    /** 한 사용자의 모든 refresh 세션을 폐기한다(탈퇴 등 전체 로그아웃용). */
    public void revokeAll(Long userId) {
        if (authTokenProperties.isRedisEnabled()) {
            String sessionsKey = SESSIONS_KEY_PREFIX + userId;
            Set<String> tokenHashes = redisTemplate.opsForSet().members(sessionsKey);
            if (tokenHashes != null && !tokenHashes.isEmpty()) {
                Set<String> refreshKeys = tokenHashes.stream()
                        .map(hash -> REFRESH_KEY_PREFIX + hash)
                        .collect(Collectors.toSet());
                redisTemplate.delete(refreshKeys);
            }
            redisTemplate.delete(sessionsKey);
            return;
        }
        refreshTokenSessionRepository.deleteByUserId(userId);
    }

    /** DB 경로에서 만료 세션을 정리한다. Redis 경로는 키별 TTL로 자동 만료되므로 no-op. */
    public void purgeExpired(Instant now) {
        if (!authTokenProperties.isRedisEnabled()) {
            refreshTokenSessionRepository.deleteExpired(now);
        }
    }

    private Optional<Long> parseUserId(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(raw));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
