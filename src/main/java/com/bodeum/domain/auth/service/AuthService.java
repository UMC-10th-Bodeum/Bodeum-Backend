package com.bodeum.domain.auth.service;

import com.bodeum.domain.auth.dto.response.AuthLoginResponse;
import com.bodeum.domain.auth.dto.response.AuthTokenResponse;
import com.bodeum.domain.auth.enumtype.AuthNextStep;
import com.bodeum.domain.auth.enumtype.SocialProvider;
import com.bodeum.domain.user.entity.UserAccount;
import com.bodeum.domain.user.service.UserAccountStore;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final OAuthProperties oAuthProperties;
    private final UserAccountStore userAccountStore;
    private final AuthTokenService authTokenService;
    private final SocialOAuthClient socialOAuthClient;
    private final OAuthStateStore oAuthStateStore;

    public URI createLoginRedirectUri(SocialProvider provider) {
        OAuthProperties.ProviderRegistration registration = oAuthProperties.getRegistration(provider);
        if (registration == null || !registration.isConfigured()) {
            throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
        }

        String redirectUri = resolveRedirectUri(provider, registration);
        String scope = StringUtils.hasText(registration.getScope())
                ? registration.getScope()
                : provider.getDefaultScope();

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(provider.getAuthorizationUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", registration.getClientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", oAuthStateStore.issue(provider));

        if (StringUtils.hasText(scope)) {
            builder.queryParam("scope", scope);
        }

        return builder.encode().build().toUri();
    }

    public AuthLoginResponse loginWithCallback(SocialProvider provider, String code, String state) {
        if (!StringUtils.hasText(code)) {
            throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
        }

        validateState(provider, state);

        SocialUserProfile socialUserProfile = socialOAuthClient.getUserProfile(provider, code, state);
        UserAccountStore.UserCreationResult userCreationResult = userAccountStore.getOrCreateSocialUser(
                provider,
                socialUserProfile.providerUserId(),
                socialUserProfile.email(),
                socialUserProfile.nickname()
        );

        UserAccount userAccount = userCreationResult.userAccount();
        AuthTokenService.AuthTokenPair tokenPair = authTokenService.issueTokens(userAccount.getId());

        return AuthLoginResponse.of(
                userAccount,
                tokenPair,
                userCreationResult.created(),
                resolveNextStep(userAccount)
        );
    }

    public AuthTokenResponse refresh(String refreshToken) {
        return AuthTokenResponse.from(authTokenService.refresh(refreshToken));
    }

    public void logout(String refreshToken) {
        authTokenService.revoke(refreshToken);
    }

    private void validateState(SocialProvider provider, String state) {
        OAuthProperties.ProviderRegistration registration = oAuthProperties.getRegistration(provider);

        // 실제 소셜 연동이 구성된 경우에만 state를 검증한다.
        // 모의 로그인은 리다이렉트 없이 콜백만 호출한다.
        if (registration != null && registration.isConfigured() && !oAuthStateStore.consume(provider, state)) {
            log.warn("[AUTH] state 검증 실패 provider={} statePresent={}", provider, StringUtils.hasText(state));
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }
    }

    private String resolveRedirectUri(SocialProvider provider, OAuthProperties.ProviderRegistration registration) {
        if (StringUtils.hasText(registration.getRedirectUri())) {
            return registration.getRedirectUri();
        }

        return oAuthProperties.getBaseUrl() + "/api/v1/auth/callback/" + provider.getPath();
    }

    private AuthNextStep resolveNextStep(UserAccount userAccount) {
        if (!userAccount.isAgreementCompleted()) {
            return AuthNextStep.TERMS;
        }

        if (!userAccount.isOnboardingCompleted()) {
            return AuthNextStep.ONBOARDING;
        }

        return AuthNextStep.HOME;
    }
}
