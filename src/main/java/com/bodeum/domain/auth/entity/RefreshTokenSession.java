package com.bodeum.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
        name = "auth_refresh_token_sessions",
        indexes = {
                @Index(name = "idx_auth_refresh_token_sessions_user_id", columnList = "user_id"),
                @Index(name = "idx_auth_refresh_token_sessions_family_id", columnList = "family_id")
        }
)
public class RefreshTokenSession {

    @Id
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    protected RefreshTokenSession() {
    }

    private RefreshTokenSession(String tokenHash, Long userId, String familyId, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
    }

    public static RefreshTokenSession create(
            String tokenHash,
            Long userId,
            String familyId,
            Instant expiresAt
    ) {
        return new RefreshTokenSession(tokenHash, userId, familyId, expiresAt);
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Long getUserId() {
        return userId;
    }

    public String getFamilyId() {
        return familyId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public void consume(Instant consumedAt) {
        this.consumedAt = consumedAt;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
