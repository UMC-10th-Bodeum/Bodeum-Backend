package com.bodeum.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.auth.repository.OAuthStateRepository;
import com.bodeum.domain.auth.repository.RefreshTokenSessionRepository;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.repository.UserRepository;
import com.bodeum.domain.user.service.UserService;
import com.bodeum.global.apiPayload.exception.ProjectException;
import com.bodeum.global.auth.AuthUserPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "bodeum.auth.jwt-secret=test-jwt-secret-32-bytes-minimum-value")
class AuthTokenServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenSessionRepository refreshTokenSessionRepository;

    @Autowired
    private OAuthStateRepository oAuthStateRepository;

    @BeforeEach
    void setUp() {
        refreshTokenSessionRepository.deleteAll();
        oAuthStateRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void issueTokensAndAuthenticate() {
        User user = createUser();

        AuthTokenService.AuthTokenPair tokenPair = authTokenService.issueTokens(user.getId());

        Optional<AuthUserPrincipal> principal = authTokenService.authenticate(tokenPair.accessToken());

        assertThat(principal).isPresent();
        assertThat(principal.get().userId()).isEqualTo(user.getId());
        assertThat(principal.get().provider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(principal.get().nickname()).isEqualTo("민준맘");
    }

    @Test
    void concurrentFirstLoginCreatesOnlyOneUser() throws Exception {
        int attemptCount = 8;
        ExecutorService executorService = Executors.newFixedThreadPool(attemptCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            List<Future<UserService.UserCreationResult>> futures = IntStream.range(0, attemptCount)
                    .mapToObj(index -> executorService.submit(() -> {
                        startLatch.await();
                        return userService.getOrCreateSocialUser(
                                SocialProvider.KAKAO,
                                "same-provider-user",
                                "parent@example.com",
                                "민준맘"
                        );
                    }))
                    .toList();

            startLatch.countDown();

            List<UserService.UserCreationResult> results = futures.stream()
                    .map(future -> {
                        try {
                            return future.get(5, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            throw new AssertionError(e);
                        }
                    })
                    .toList();

            assertThat(userRepository.count()).isEqualTo(1);
            assertThat(results).filteredOn(UserService.UserCreationResult::created).hasSize(1);
            assertThat(results)
                    .extracting(UserService.UserCreationResult::userId)
                    .containsOnly(results.getFirst().userId());
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
        User user = createUser();
        String tokenSignedWithOtherKey = otherKeyProvider.createAccessToken(
                user.getAuthSubject(),
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
        User user = createUser();
        AuthTokenService.AuthTokenPair tokenPair = authTokenService.issueTokens(user.getId());

        AuthTokenService.AuthTokenPair refreshedTokenPair = authTokenService.refresh(tokenPair.refreshToken());

        assertThat(refreshedTokenPair.accessToken()).isNotEqualTo(tokenPair.accessToken());
        assertThat(refreshedTokenPair.refreshToken()).isNotEqualTo(tokenPair.refreshToken());
    }

    @Test
    void refreshRotatesRefreshToken() {
        User user = createUser();
        AuthTokenService.AuthTokenPair tokenPair = authTokenService.issueTokens(user.getId());

        authTokenService.refresh(tokenPair.refreshToken());

        // 이미 사용한 refresh token은 재사용할 수 없다.
        assertThatThrownBy(() -> authTokenService.refresh(tokenPair.refreshToken()))
                .isInstanceOf(ProjectException.class);
    }

    @Test
    void refreshTokenIsStoredAsHash() {
        User user = createUser();

        AuthTokenService.AuthTokenPair tokenPair = authTokenService.issueTokens(user.getId());

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
        User user = createUser();
        AuthTokenService.AuthTokenPair tokenPair = authTokenService.issueTokens(user.getId());

        userService.withdraw(user.getId(), null);

        assertThatThrownBy(() -> authTokenService.refresh(tokenPair.refreshToken()))
                .isInstanceOf(ProjectException.class);
    }

    @Test
    void authenticateFailsForWithdrawnUser() {
        User user = createUser();
        AuthTokenService.AuthTokenPair tokenPair = authTokenService.issueTokens(user.getId());

        userService.withdraw(user.getId(), null);

        assertThat(authTokenService.authenticate(tokenPair.accessToken())).isEmpty();
    }

    @Test
    void revokeRemovesRefreshToken() {
        User user = createUser();
        AuthTokenService.AuthTokenPair tokenPair = authTokenService.issueTokens(user.getId());

        authTokenService.revoke(tokenPair.refreshToken());

        assertThatThrownBy(() -> authTokenService.refresh(tokenPair.refreshToken()))
                .isInstanceOf(ProjectException.class);
    }

    private User createUser() {
        Long userId = userService
                .getOrCreateSocialUser(SocialProvider.KAKAO, "kakao-user-1", "parent@example.com", "민준맘")
                .userId();
        return userService.getUserById(userId);
    }
}
