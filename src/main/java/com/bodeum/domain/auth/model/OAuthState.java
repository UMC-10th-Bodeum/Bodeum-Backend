package com.bodeum.domain.auth.model;

import com.bodeum.domain.auth.enumtype.SocialProvider;
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

    protected OAuthState() {
    }

    private OAuthState(String state, SocialProvider provider, Instant expiresAt) {
        this.state = state;
        this.provider = provider;
        this.expiresAt = expiresAt;
    }

    public static OAuthState create(String state, SocialProvider provider, Instant expiresAt) {
        return new OAuthState(state, provider, expiresAt);
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

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
