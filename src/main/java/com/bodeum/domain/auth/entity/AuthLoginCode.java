package com.bodeum.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 소셜 로그인 완료 후 프론트로 리다이렉트할 때 발급하는 일회용 로그인 code.
 * 프론트가 code를 교환하면 토큰을 발급하고 즉시 소비(삭제)한다.
 */
@Entity
@Table(name = "auth_login_codes")
public class AuthLoginCode {

    @Id
    @Column(nullable = false, length = 64)
    private String code;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "is_new_user", nullable = false)
    private boolean newUser;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected AuthLoginCode() {
    }

    private AuthLoginCode(String code, Long userId, boolean newUser, Instant expiresAt) {
        this.code = code;
        this.userId = userId;
        this.newUser = newUser;
        this.expiresAt = expiresAt;
    }

    public static AuthLoginCode create(String code, Long userId, boolean newUser, Instant expiresAt) {
        return new AuthLoginCode(code, userId, newUser, expiresAt);
    }

    public String getCode() {
        return code;
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isNewUser() {
        return newUser;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
