package com.bodeum.domain.auth.service;

import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.auth.entity.OAuthState;
import com.bodeum.domain.auth.repository.OAuthStateRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * OAuth 로그인 CSRF 방지용 state 저장소.
 * 리다이렉트 시 발급한 state만 콜백에서 1회 소비할 수 있다.
 * state에는 로그인 완료 후 돌아갈 프론트 콜백 URL을 함께 실어 콜백까지 왕복시킨다.
 */
@Component
@RequiredArgsConstructor
public class OAuthStateStore {

    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "bodeum:auth:oauth-state:";

    /**
     * Redis 값에 provider와 프론트 콜백 URL을 함께 담기 위한 구분자.
     * getAndDelete 한 번으로 원자적으로 소비하려고 Hash 대신 구분자 문자열을 쓴다.
     * provider 이름(KAKAO/NAVER)에는 이 문자가 없으므로 첫 구분자 기준으로 안전하게 나뉜다.
     */
    private static final String VALUE_DELIMITER = "|";

    private final OAuthStateRepository oAuthStateRepository;
    private final AuthTokenProperties authTokenProperties;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public String issue(SocialProvider provider, String frontCallbackUrl) {
        String state = UUID.randomUUID().toString();

        if (authTokenProperties.isRedisEnabled()) {
            redisTemplate.opsForValue().set(KEY_PREFIX + state, encode(provider, frontCallbackUrl), STATE_TTL);
            return state;
        }

        purgeExpired();
        oAuthStateRepository.save(
                OAuthState.create(state, provider, Instant.now().plus(STATE_TTL), frontCallbackUrl)
        );

        return state;
    }

    @Transactional
    public boolean consume(SocialProvider provider, String state) {
        if (!StringUtils.hasText(state)) {
            return false;
        }

        if (authTokenProperties.isRedisEnabled()) {
            String storedValue = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + state);
            return storedValue != null && provider.name().equals(decodeProvider(storedValue));
        }

        Instant now = Instant.now();
        OAuthState entry = oAuthStateRepository.findByStateForUpdate(state)
                .orElse(null);
        if (entry == null) {
            return false;
        }

        oAuthStateRepository.delete(entry);

        return entry.getProvider() == provider && !entry.isExpired(now);
    }

    /**
     * state를 소비하지 않고 프론트 콜백 URL만 읽는다.
     * 콜백이 실패해도 프론트로 에러를 돌려보내야 하는데, 그 시점에는 state가 이미 소비돼
     * 사라진 뒤라서 목적지를 미리 확보해 두기 위한 조회다.
     */
    @Transactional(readOnly = true)
    public Optional<String> findFrontCallbackUrl(String state) {
        if (!StringUtils.hasText(state)) {
            return Optional.empty();
        }

        if (authTokenProperties.isRedisEnabled()) {
            return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + state))
                    .map(this::decodeFrontCallbackUrl)
                    .filter(StringUtils::hasText);
        }

        return oAuthStateRepository.findById(state)
                .map(OAuthState::getFrontCallbackUrl)
                .filter(StringUtils::hasText);
    }

    private String encode(SocialProvider provider, String frontCallbackUrl) {
        return provider.name() + VALUE_DELIMITER + (frontCallbackUrl == null ? "" : frontCallbackUrl);
    }

    private String decodeProvider(String storedValue) {
        int delimiterIndex = storedValue.indexOf(VALUE_DELIMITER);
        return delimiterIndex < 0 ? storedValue : storedValue.substring(0, delimiterIndex);
    }

    private String decodeFrontCallbackUrl(String storedValue) {
        int delimiterIndex = storedValue.indexOf(VALUE_DELIMITER);
        return delimiterIndex < 0 ? null : storedValue.substring(delimiterIndex + 1);
    }

    private void purgeExpired() {
        oAuthStateRepository.deleteExpired(Instant.now());
    }
}
