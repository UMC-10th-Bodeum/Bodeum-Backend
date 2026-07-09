package com.bodeum.domain.auth.service;

import com.bodeum.domain.auth.enumtype.SocialProvider;
import com.bodeum.domain.auth.model.OAuthState;
import com.bodeum.domain.auth.repository.OAuthStateRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * OAuth 로그인 CSRF 방지용 state 저장소.
 * 리다이렉트 시 발급한 state만 콜백에서 1회 소비할 수 있다.
 */
@Component
@RequiredArgsConstructor
public class OAuthStateStore {

    private static final Duration STATE_TTL = Duration.ofMinutes(10);

    private final OAuthStateRepository oAuthStateRepository;

    @Transactional
    public String issue(SocialProvider provider) {
        purgeExpired();

        String state = UUID.randomUUID().toString();
        oAuthStateRepository.save(OAuthState.create(state, provider, Instant.now().plus(STATE_TTL)));

        return state;
    }

    @Transactional
    public boolean consume(SocialProvider provider, String state) {
        if (!StringUtils.hasText(state)) {
            return false;
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

    private void purgeExpired() {
        oAuthStateRepository.deleteExpired(Instant.now());
    }
}
