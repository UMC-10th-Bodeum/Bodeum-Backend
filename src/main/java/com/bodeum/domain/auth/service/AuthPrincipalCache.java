package com.bodeum.domain.auth.service;

import com.bodeum.global.auth.AuthUserPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 매 요청 인증 시 authSubject → 사용자 principal 조회 결과를 Redis에 캐시한다.
 * DB가 원본이므로 순수 성능 캐시이며, redis-enabled=false면 전부 no-op(캐시 미사용)이다.
 *
 * <p>장애 정책: Redis 장애 시 <b>캐시 미스로 취급(fail-safe)</b>해 호출자가 DB로 폴백한다.
 * 캐시는 최적화일 뿐이라 가용성을 인증 성공보다 우선할 이유가 없다.
 *
 * <p>stale 방지를 위해 principal 필드(nickname 등)가 바뀌는 지점(프로필 수정)과
 * 사용자가 비활성화되는 지점(탈퇴)에서 반드시 evict 해야 한다.
 *
 * <p>직렬화는 웹 스택의 ObjectMapper 대신 전용 인스턴스를 쓴다. 캐시 포맷을 웹 Jackson 설정
 * 변경(모듈 추가 등)으로부터 격리하고, 일부 테스트 컨텍스트에 ObjectMapper 빈이 없는 문제도 피한다.
 * (AuthUserPrincipal은 단순 record라 추가 모듈이 필요 없다.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthPrincipalCache {

    private static final String KEY_PREFIX = "bodeum:auth:principal:";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AuthTokenProperties authTokenProperties;
    private final StringRedisTemplate redisTemplate;

    public Optional<AuthUserPrincipal> get(String authSubject) {
        if (!authTokenProperties.isRedisEnabled() || authSubject == null) {
            return Optional.empty();
        }

        String json;
        try {
            json = redisTemplate.opsForValue().get(KEY_PREFIX + authSubject);
        } catch (DataAccessException e) {
            // Redis 장애 → 캐시 미스로 취급해 DB 폴백.
            log.warn("[AUTH] principal 캐시 조회 실패 (Redis 장애) - DB 폴백 authSubject={}", authSubject, e);
            return Optional.empty();
        }
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(OBJECT_MAPPER.readValue(json, AuthUserPrincipal.class));
        } catch (JsonProcessingException e) {
            // 손상된 캐시 값은 버리고 캐시 미스로 처리한다.
            deleteQuietly(authSubject);
            return Optional.empty();
        }
    }

    public void put(String authSubject, AuthUserPrincipal principal) {
        if (!authTokenProperties.isRedisEnabled() || authSubject == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + authSubject,
                    OBJECT_MAPPER.writeValueAsString(principal),
                    authTokenProperties.getPrincipalCacheTtl()
            );
        } catch (JsonProcessingException e) {
            // 캐시는 최적화일 뿐이므로 직렬화 실패 시 캐시하지 않고 넘어간다.
        } catch (DataAccessException e) {
            // Redis 장애 시 캐시 적재를 조용히 건너뛴다(다음 요청은 DB 폴백).
            log.warn("[AUTH] principal 캐시 적재 실패 (Redis 장애) authSubject={}", authSubject, e);
        }
    }

    public void evict(String authSubject) {
        if (!authTokenProperties.isRedisEnabled() || authSubject == null) {
            return;
        }
        deleteQuietly(authSubject);
    }

    private void deleteQuietly(String authSubject) {
        try {
            redisTemplate.delete(KEY_PREFIX + authSubject);
        } catch (DataAccessException e) {
            // evict 실패 시 stale이 최대 principalCacheTtl 동안 남을 수 있으나 인증 흐름은 막지 않는다.
            log.warn("[AUTH] principal 캐시 삭제 실패 (Redis 장애) authSubject={}", authSubject, e);
        }
    }
}
