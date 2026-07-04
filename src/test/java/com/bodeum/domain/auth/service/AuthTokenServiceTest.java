package com.bodeum.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bodeum.domain.auth.enumtype.SocialProvider;
import com.bodeum.domain.user.model.UserAccount;
import com.bodeum.domain.user.service.UserAccountStore;
import com.bodeum.global.apiPayload.exception.ProjectException;
import com.bodeum.global.auth.AuthUserPrincipal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthTokenServiceTest {

    private final UserAccountStore userAccountStore = new UserAccountStore();
    private final AuthTokenProperties authTokenProperties = new AuthTokenProperties();
    private final AuthTokenService authTokenService = new AuthTokenService(
            userAccountStore,
            new JwtTokenProvider(authTokenProperties),
            authTokenProperties
    );

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
    void authenticateFailsWithForgedToken() {
        assertThat(authTokenService.authenticate("forged-access-token")).isEmpty();

        JwtTokenProvider otherKeyProvider = new JwtTokenProvider(new AuthTokenProperties());
        String tokenSignedWithOtherKey = otherKeyProvider.createAccessToken(
                createUser().getId(),
                Instant.now(),
                Instant.now().plusSeconds(600)
        );

        assertThat(authTokenService.authenticate(tokenSignedWithOtherKey)).isEmpty();
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
    void refreshFailsWithInvalidRefreshToken() {
        assertThatThrownBy(() -> authTokenService.refresh("invalid-refresh-token"))
                .isInstanceOf(ProjectException.class);
    }

    @Test
    void refreshFailsForWithdrawnUser() {
        UserAccount userAccount = createUser();
        AuthTokenService.AuthTokenPair tokenPair = authTokenService.issueTokens(userAccount.getId());

        userAccount.withdraw();

        assertThatThrownBy(() -> authTokenService.refresh(tokenPair.refreshToken()))
                .isInstanceOf(ProjectException.class);
    }

    @Test
    void authenticateFailsForWithdrawnUser() {
        UserAccount userAccount = createUser();
        AuthTokenService.AuthTokenPair tokenPair = authTokenService.issueTokens(userAccount.getId());

        userAccount.withdraw();

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
