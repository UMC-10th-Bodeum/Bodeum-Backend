package com.bodeum.domain.auth.service;

import com.bodeum.domain.auth.enumtype.SocialProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * OAuth 로그인 CSRF 방지용 state 저장소.
 * 리다이렉트 시 발급한 state만 콜백에서 1회 소비할 수 있다.
 */
@Component
public class OAuthStateStore {

    private static final Duration STATE_TTL = Duration.ofMinutes(10);

    private final ConcurrentMap<String, StateEntry> states = new ConcurrentHashMap<>();

    public String issue(SocialProvider provider) {
        purgeExpired();

        String state = UUID.randomUUID().toString();
        states.put(state, new StateEntry(provider, Instant.now().plus(STATE_TTL)));

        return state;
    }

    public boolean consume(SocialProvider provider, String state) {
        if (!StringUtils.hasText(state)) {
            return false;
        }

        StateEntry entry = states.remove(state);

        return entry != null && entry.provider() == provider && !entry.isExpired();
    }

    private void purgeExpired() {
        states.values().removeIf(StateEntry::isExpired);
    }

    private record StateEntry(SocialProvider provider, Instant expiresAt) {

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
