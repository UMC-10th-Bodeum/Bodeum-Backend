package com.bodeum.domain.auth.service;

import com.bodeum.domain.auth.entity.RefreshTokenSession;
import com.bodeum.domain.auth.repository.RefreshTokenSessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * refresh 토큰 세션 저장소. redis-enabled면 Redis(키별 TTL), 아니면 DB(JPA)에 저장한다.
 *
 * <p>각 로그인은 고유 session-family를 가지며 refresh 회전 뒤에도 familyId를 유지한다.
 * 기기별 로그아웃은 해당 family만 폐기하고, 탈퇴는 유저별 역인덱스로 모든 family를 폐기한다.
 *
 * <p>Redis 경로에서 소비된 토큰은 즉시 삭제하지 않고 TTL 동안 tombstone(C 상태)으로 남긴다.
 * logout이 refresh 회전과 경쟁해 옛 토큰을 받더라도 familyId를 찾아 현재 회전 토큰까지
 * 폐기할 수 있게 하기 위해서다.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String REFRESH_KEY_PREFIX = "bodeum:auth:refresh:";
    private static final String SESSIONS_KEY_PREFIX = "bodeum:auth:user-sessions:";
    private static final String FAMILY_KEY_PREFIX = "bodeum:auth:refresh-family:";
    private static final String REVOKED_FAMILY_KEY_PREFIX = "bodeum:auth:refresh-family-revoked:";
    private static final String VALUE_DELIMITER = ":";
    private static final String ACTIVE_STATUS = "A";

    // family 폐기 여부 확인 + refresh 저장 + 역인덱스·family 현재 토큰 갱신을 원자적으로 수행한다.
    // KEYS[1]=refresh, KEYS[2]=user sessions, KEYS[3]=family, KEYS[4]=revoked family
    // ARGV[1]=session value, ARGV[2]=ttl(ms), ARGV[3]=tokenHash, ARGV[4]=family value
    private static final RedisScript<Long> SAVE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('EXISTS', KEYS[4]) == 1 then return 0 end "
                    + "redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2]) "
                    + "redis.call('SADD', KEYS[2], ARGV[3]) "
                    + "redis.call('PEXPIRE', KEYS[2], ARGV[2]) "
                    + "redis.call('SET', KEYS[3], ARGV[4], 'PX', ARGV[2]) "
                    + "return 1",
            Long.class);

    // 활성 refresh를 1회 소비하고 같은 키를 consumed tombstone으로 전환한다.
    // KEYS[1]=refresh / ARGV[1]=sessions prefix, ARGV[2]=tokenHash
    private static final RedisScript<String> CONSUME_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]) "
                    + "if not value or string.sub(value, 1, 2) ~= 'A:' then return nil end "
                    + "local userId = string.match(value, '^A:([^:]+):') "
                    + "redis.call('SET', KEYS[1], 'C:' .. string.sub(value, 3), 'KEEPTTL') "
                    + "redis.call('SREM', ARGV[1] .. userId, ARGV[2]) "
                    + "return value",
            String.class);

    // 전달된 토큰이 active/consumed 어느 상태든 family를 찾아 현재 회전 토큰까지 폐기한다.
    // KEYS[1]=전달된 refresh 키
    // ARGV[1]=expected userId(빈 문자열이면 소유자 검사 생략), ARGV[2]=tokenHash,
    // ARGV[3..6]=refresh/sessions/family/revoked-family prefix, ARGV[7]=revoked TTL(ms)
    private static final RedisScript<Long> REVOKE_FAMILY_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]) "
                    + "if not value then return 0 end "
                    + "local storedUserId, familyId = string.match(value, '^[AC]:([^:]+):([^:]+):') "
                    + "if not storedUserId or (ARGV[1] ~= '' and storedUserId ~= ARGV[1]) then return 0 end "
                    + "local familyKey = ARGV[5] .. familyId "
                    + "local familyValue = redis.call('GET', familyKey) "
                    + "local activeHash = nil "
                    + "if familyValue then "
                    + "  local familyOwner "
                    + "  familyOwner, activeHash = string.match(familyValue, '^([^:]+):([^:]+)$') "
                    + "  if familyOwner ~= storedUserId then return 0 end "
                    + "end "
                    + "redis.call('SET', ARGV[6] .. familyId, '1', 'PX', ARGV[7]) "
                    + "if activeHash then "
                    + "  redis.call('DEL', ARGV[3] .. activeHash) "
                    + "  redis.call('SREM', ARGV[4] .. storedUserId, activeHash) "
                    + "end "
                    + "redis.call('DEL', KEYS[1]) "
                    + "redis.call('SREM', ARGV[4] .. storedUserId, ARGV[2]) "
                    + "redis.call('DEL', familyKey) "
                    + "return 1",
            Long.class);

    // 역인덱스의 모든 활성 refresh 키와 family 키를 원자적으로 삭제한다.
    // KEYS[1]=sessions / ARGV[1]=refresh prefix, ARGV[2]=family prefix
    private static final RedisScript<Long> REVOKE_ALL_SCRIPT = new DefaultRedisScript<>(
            "local members = redis.call('SMEMBERS', KEYS[1]) "
                    + "for i=1,#members do "
                    + "  local value = redis.call('GET', ARGV[1] .. members[i]) "
                    + "  if value then "
                    + "    local familyId = string.match(value, '^[AC]:[^:]+:([^:]+):') "
                    + "    if familyId then redis.call('DEL', ARGV[2] .. familyId) end "
                    + "  end "
                    + "  redis.call('DEL', ARGV[1] .. members[i]) "
                    + "end "
                    + "redis.call('DEL', KEYS[1]) "
                    + "return #members",
            Long.class);

    private final AuthTokenProperties authTokenProperties;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * refresh 세션을 저장한다.
     *
     * @return family가 로그아웃으로 폐기되지 않아 저장됐으면 true
     */
    public boolean save(
            String tokenHash,
            Long userId,
            String familyId,
            Instant expiresAt,
            Duration ttl
    ) {
        if (authTokenProperties.isRedisEnabled()) {
            Long saved = redisTemplate.execute(
                    SAVE_SCRIPT,
                    List.of(
                            REFRESH_KEY_PREFIX + tokenHash,
                            SESSIONS_KEY_PREFIX + userId,
                            FAMILY_KEY_PREFIX + familyId,
                            REVOKED_FAMILY_KEY_PREFIX + familyId
                    ),
                    serializeSession(ACTIVE_STATUS, userId, familyId, expiresAt),
                    Long.toString(ttl.toMillis()),
                    tokenHash,
                    userId + VALUE_DELIMITER + tokenHash);
            return Long.valueOf(1L).equals(saved);
        }

        refreshTokenSessionRepository.save(
                RefreshTokenSession.create(tokenHash, userId, familyId, expiresAt));
        return true;
    }

    /**
     * refresh 토큰을 1회 소비하고 소유자와 session-family를 반환한다.
     * 소비된 토큰은 family 로그아웃 경쟁을 처리하기 위해 만료 시각까지 tombstone으로 유지한다.
     */
    public Optional<ConsumedSession> consume(String tokenHash, Instant now) {
        if (authTokenProperties.isRedisEnabled()) {
            String value = redisTemplate.execute(
                    CONSUME_SCRIPT,
                    List.of(REFRESH_KEY_PREFIX + tokenHash),
                    SESSIONS_KEY_PREFIX,
                    tokenHash);
            return parseActiveSession(value, now);
        }

        RefreshTokenSession session = refreshTokenSessionRepository.findByTokenHashForUpdate(tokenHash)
                .orElse(null);
        if (session == null || session.isConsumed() || session.isExpired(now)) {
            if (session != null && session.isExpired(now)) {
                refreshTokenSessionRepository.delete(session);
            }
            return Optional.empty();
        }

        session.consume(now);
        return Optional.of(new ConsumedSession(
                session.getUserId(),
                session.getFamilyId(),
                session.getExpiresAt()
        ));
    }

    /** 토큰의 session-family만 폐기한다(기기별 로그아웃). */
    public void revokeFamily(Long userId, String tokenHash) {
        if (authTokenProperties.isRedisEnabled()) {
            redisTemplate.execute(
                    REVOKE_FAMILY_SCRIPT,
                    List.of(REFRESH_KEY_PREFIX + tokenHash),
                    userId == null ? "" : userId.toString(),
                    tokenHash,
                    REFRESH_KEY_PREFIX,
                    SESSIONS_KEY_PREFIX,
                    FAMILY_KEY_PREFIX,
                    REVOKED_FAMILY_KEY_PREFIX,
                    Long.toString(authTokenProperties.getRefreshTokenTtl().toMillis()));
            return;
        }

        refreshTokenSessionRepository.findByTokenHashForUpdate(tokenHash)
                .filter(session -> userId == null || session.getUserId().equals(userId))
                .ifPresent(session -> refreshTokenSessionRepository.deleteByUserIdAndFamilyId(
                        session.getUserId(),
                        session.getFamilyId()
                ));
    }

    /** 한 사용자의 모든 refresh 세션을 폐기한다(탈퇴용). */
    public void revokeAll(Long userId) {
        if (authTokenProperties.isRedisEnabled()) {
            redisTemplate.execute(
                    REVOKE_ALL_SCRIPT,
                    List.of(SESSIONS_KEY_PREFIX + userId),
                    REFRESH_KEY_PREFIX,
                    FAMILY_KEY_PREFIX);
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

    private String serializeSession(
            String status,
            Long userId,
            String familyId,
            Instant expiresAt
    ) {
        return status
                + VALUE_DELIMITER + userId
                + VALUE_DELIMITER + familyId
                + VALUE_DELIMITER + expiresAt.toEpochMilli();
    }

    private Optional<ConsumedSession> parseActiveSession(String raw, Instant now) {
        if (raw == null) {
            return Optional.empty();
        }

        String[] parts = raw.split(VALUE_DELIMITER, -1);
        if (parts.length != 4 || !ACTIVE_STATUS.equals(parts[0])) {
            return Optional.empty();
        }

        try {
            Long userId = Long.valueOf(parts[1]);
            Instant expiresAt = Instant.ofEpochMilli(Long.parseLong(parts[3]));
            if (!expiresAt.isAfter(now)) {
                return Optional.empty();
            }
            return Optional.of(new ConsumedSession(userId, parts[2], expiresAt));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public record ConsumedSession(Long userId, String familyId, Instant expiresAt) {
    }
}
