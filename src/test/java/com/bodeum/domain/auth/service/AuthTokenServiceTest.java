package com.bodeum.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bodeum.domain.auth.enumtype.SocialProvider;
import com.bodeum.domain.auth.repository.OAuthStateRepository;
import com.bodeum.domain.auth.repository.RefreshTokenSessionRepository;
import com.bodeum.domain.user.model.UserAccount;
import com.bodeum.domain.user.repository.UserAccountRepository;
import com.bodeum.domain.user.service.UserAccountStore;
import com.bodeum.global.apiPayload.exception.ProjectException;
import com.bodeum.global.auth.AuthUserPrincipal;
import java.util.List;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "bodeum.auth.jwt-secret=test-jwt-secret-32-bytes-minimum-value")
class AuthTokenServiceTest {

    @Autowired
    private UserAccountStore userAccountStore;

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RefreshTokenSessionRepository refreshTokenSessionRepository;

    @Autowired
    private OAuthStateRepository oAuthStateRepository;

    @BeforeEach
    void setUp() {
        refreshTokenSessionRepository.deleteAll();
        oAuthStateRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void issueTokensAndAuthenticate() {
        UserAccount userAccount = createUser();

        AuthTokenService.AuthTokenPair tokenPair = authTokenService.issueTokens(userAccount.getId());

        Optional<AuthUserPrincipal> principal = authTokenService.authenticate(tokenPair.accessToken());

        assertThat(principal).isPresent();
        assertThat(principal.get().userId()).isEqualTo(userAccount.getId());
        assertThat(principal.get().provider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(principal.get().nickname()).isEqualTo("민준맘");
    }

    @Test
    void concurrentFirstLoginCreatesOnlyOneUser() throws Exception {
        int attemptCount = 8;
        ExecutorService executorService = Executors.newFixedThreadPool(attemptCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            List<Future<UserAccountStore.UserCreationResult>> futures = java.util.stream.IntStream.range(0, attemptCount)
                    .mapToObj(index -> executorService.submit(() -> {
                        startLatch.await();
                        return userAccountStore.getOrCreateSocialUser(
                                SocialProvider.KAKAO,
                                "same-provider-user",
                                "parent@example.com",
                                "민준맘"
                        );
                    }))
                    .toList();

            startLatch.countDown();

            List<UserAccountStore.UserCreationResult> results = futures.stream()
                    .map(future -> {
                        try {
                            return future.get(5, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            throw new AssertionError(e);
                        }
                    })
                    .toList();

            assertThat(userAccountRepository.count()).isEqualTo(1);
            assertThat(results).filteredOn(UserAccountStore.UserCreationResult::created).hasSize(1);
            assertThat(results).extracting(result -> result.userAccount().getId()).containsOnly(results.getFirst().userAccount().getId());
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void authenticateFailsWithForgedToken() {
        assertThat(authTokenService.authenticate("forged-access-token")).isEmpty();

        AuthTokenProperties otherProperties = new AuthTokenProperties();
        otherProperties.setJwtSecret("other-jwt-secret-32-bytes-minimum");
        JwtTokenProvider otherKeyProvider = new JwtTokenProvider(otherProperties);
        UserAccount userAccount = createUser();
        String tokenSignedWithOtherKey = otherKeyProvider.createAccessToken(
                userAccount.getAuthSubject(),
                Instant.now(),
                Instant.now().plusSeconds(600)
        );

        assertThat(authTokenService.authenticate(tokenSignedWithOtherKey)).isEmpty();
    }

    @Test
    void authenticateFailsWhenJwtSubjectIsMissing() {
        String tokenWithoutSubject = jwtTokenProvider.createAccessToken(
                null,
                Instant.now(),
                Instant.now().plusSeconds(600)
        );

        assertThat(authTokenService.authenticate(tokenWithoutSubject)).isEmpty();
    }

    @Test
    void refreshIssuesNewTokenPair() {
        UserAccount userAccount = createUser();
        AuthTokenService.AuthTokenPair tokenPair = authTokenService.issueTokens(userAccount.getId());

        AuthTokenService.AuthTokenPair refreshedTokenPair = authTokenService.refresh(tokenPair.refreshToken());

        assertThat(refreshedTokenPair.accessToken()).isNotEqualTo(tokenPair.accessToken());
        assertThat(refreshedTokenPair.refreshToken()).isNotEqualTo(tokenPair.refreshToken());
    }

    @Test
    void refreshRotatesRefreshToken() {
        UserAccount userAccount = createUser();
        AuthTokenService.AuthTokenPair tokenPair = authTokenService.issueTokens(userAccount.getId());

        authTokenService.refresh(tokenPair.refreshToken());

        // 이미 사용한 refresh token은 재사용할 수 없다.
        assertThatThrownBy(() -> authTokenService.refresh(tokenPair.refreshToken()))
                .isInstanceOf(ProjectException.class);
    }

    @Test
    void refreshTokenIsStoredAsHash() {
        UserAccount userAccount = createUser();

        AuthTokenService.AuthTokenPair tokenPair = authTokenService.issueTokens(userAccount.getId());

        assertThat(refreshTokenSessionRepository.findAll())
                .singleElement()
                .satisfies(session -> {
                    assertThat(session.getTokenHash()).hasSize(64);
                    assertThat(session.getTokenHash()).isNotEqualTo(tokenPair.refreshToken());
                });
    }

    @Test
    void refreshFailsWithInvalidRefreshToken() {
        assertThatThrownBy(() -> authTokenService.refresh("invalid-refresh-token"))
                .isInstanceOf(ProjectException.class);
    }

    @Test
    void refreshFailsForWithdrawnUser() {
        UserAccount userAccount = createUser();
        AuthTokenService.AuthTokenPair tokenPair = authTokenService.issueTokens(userAccount.getId());

        userAccount.withdraw();
        userAccountRepository.saveAndFlush(userAccount);

        assertThatThrownBy(() -> authTokenService.refresh(tokenPair.refreshToken()))
                .isInstanceOf(ProjectException.class);
    }

    @Test
    void authenticateFailsForWithdrawnUser() {
        UserAccount userAccount = createUser();
        AuthTokenService.AuthTokenPair tokenPair = authTokenService.issueTokens(userAccount.getId());

        userAccount.withdraw();
        userAccountRepository.saveAndFlush(userAccount);

        assertThat(authTokenService.authenticate(tokenPair.accessToken())).isEmpty();
    }

    @Test
    void revokeRemovesRefreshToken() {
        UserAccount userAccount = createUser();
        AuthTokenService.AuthTokenPair tokenPair = authTokenService.issueTokens(userAccount.getId());

        authTokenService.revoke(tokenPair.refreshToken());

        assertThatThrownBy(() -> authTokenService.refresh(tokenPair.refreshToken()))
                .isInstanceOf(ProjectException.class);
    }

    private UserAccount createUser() {
        return userAccountStore
                .getOrCreateSocialUser(SocialProvider.KAKAO, "kakao-user-1", "parent@example.com", "민준맘")
                .userAccount();
    }
}
