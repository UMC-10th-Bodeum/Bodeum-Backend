package com.bodeum.domain.auth.service;

import com.bodeum.domain.auth.entity.AuthLoginCode;
import com.bodeum.domain.auth.exception.AuthErrorCode;
import com.bodeum.domain.auth.repository.AuthLoginCodeRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 소셜 로그인 콜백 → 프론트 리다이렉트에 사용하는 일회용 로그인 code 저장소.
 * 콜백에서 발급한 code만 프론트가 1회 교환할 수 있고, 교환 즉시 소비된다.
 */
@Component
@RequiredArgsConstructor
public class AuthLoginCodeStore {

    private final AuthLoginCodeRepository authLoginCodeRepository;
    private final AuthTokenProperties authTokenProperties;

    @Transactional
    public String issue(Long userId, boolean isNewUser) {
        purgeExpired();

        String code = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(authTokenProperties.getLoginCodeTtl());
        authLoginCodeRepository.save(AuthLoginCode.create(code, userId, isNewUser, expiresAt));

        return code;
    }

    // 소비(삭제)를 별도 트랜잭션으로 먼저 커밋한다. 호출자(exchange)의 토큰 발급이 이후 실패해도
    // code 삭제가 롤백되지 않도록 해 1회용을 보장한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Consumed consume(String code) {
        if (!StringUtils.hasText(code)) {
            throw new ProjectException(AuthErrorCode.INVALID_LOGIN_CODE);
        }

        AuthLoginCode entry = authLoginCodeRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new ProjectException(AuthErrorCode.INVALID_LOGIN_CODE));

        // 조회 즉시 삭제해 1회용을 보장한다(만료 여부와 무관하게 재사용 불가).
        authLoginCodeRepository.delete(entry);

        if (entry.isExpired(Instant.now())) {
            throw new ProjectException(AuthErrorCode.INVALID_LOGIN_CODE);
        }

        return new Consumed(entry.getUserId(), entry.isNewUser());
    }

    private void purgeExpired() {
        authLoginCodeRepository.deleteExpired(Instant.now());
    }

    public record Consumed(Long userId, boolean isNewUser) {
    }
}
