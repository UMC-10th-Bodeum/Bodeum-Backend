package com.bodeum.domain.auth.entity;

import com.bodeum.domain.auth.enums.SocialProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "oauth_states")
public class OAuthState {

    @Id
    @Column(nullable = false, length = 64)
    private String state;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * 로그인 완료 후 돌아갈 프론트 콜백 URL.
     * 프론트 로컬 개발 서버마다 값이 달라 요청별로 이 state에 실어 왕복시킨다.
     * 값이 비어 있으면 서버 기본 콜백 URL을 쓴다.
     */
    @Column(name = "front_callback_url", length = 255)
    private String frontCallbackUrl;

    protected OAuthState() {
    }

    private OAuthState(String state, SocialProvider provider, Instant expiresAt, String frontCallbackUrl) {
        this.state = state;
        this.provider = provider;
        this.expiresAt = expiresAt;
        this.frontCallbackUrl = frontCallbackUrl;
    }

    public static OAuthState create(
            String state,
            SocialProvider provider,
            Instant expiresAt,
            String frontCallbackUrl
    ) {
        return new OAuthState(state, provider, expiresAt, frontCallbackUrl);
    }

    public String getState() {
        return state;
    }

    public SocialProvider getProvider() {
        return provider;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getFrontCallbackUrl() {
        return frontCallbackUrl;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
