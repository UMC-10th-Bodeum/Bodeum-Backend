package com.bodeum.domain.auth.service;

import com.bodeum.global.auth.AuthUserPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
// AuthUserPrincipal은 단순 record라 웹 스택의 ObjectMapper 설정에 의존할 필요가 없다.

/**
 * 매 요청 인증 시 authSubject → 사용자 principal 조회 결과를 Redis에 캐시한다.
 * DB가 원본이므로 순수 성능 캐시이며, redis-enabled=false면 전부 no-op(캐시 미사용)이다.
 *
 * <p>stale 방지를 위해 principal 필드(nickname 등)가 바뀌는 지점(프로필 수정)과
 * 사용자가 비활성화되는 지점(탈퇴)에서 반드시 evict 해야 한다.
 */
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

        String json = redisTemplate.opsForValue().get(KEY_PREFIX + authSubject);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(OBJECT_MAPPER.readValue(json, AuthUserPrincipal.class));
        } catch (JsonProcessingException e) {
            // 손상된 캐시 값은 버리고 캐시 미스로 처리한다.
            redisTemplate.delete(KEY_PREFIX + authSubject);
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
        }
    }

    public void evict(String authSubject) {
        if (!authTokenProperties.isRedisEnabled() || authSubject == null) {
            return;
        }
        redisTemplate.delete(KEY_PREFIX + authSubject);
    }
}
