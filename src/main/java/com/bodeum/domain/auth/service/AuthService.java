package com.bodeum.domain.auth.service;

import com.bodeum.domain.auth.dto.response.AuthLoginResponse;
import com.bodeum.domain.auth.dto.response.AuthTokenResponse;
import com.bodeum.domain.auth.enums.AuthNextStep;
import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.auth.exception.AuthErrorCode;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.service.UserService;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final OAuthProperties oAuthProperties;
    private final UserService userService;
    private final AuthTokenService authTokenService;
    private final SocialOAuthClient socialOAuthClient;
    private final OAuthStateStore oAuthStateStore;

    public URI createLoginRedirectUri(SocialProvider provider) {
        OAuthProperties.ProviderRegistration registration = oAuthProperties.getRegistration(provider);
        if (registration == null || !registration.isConfigured()) {
            throw new ProjectException(AuthErrorCode.PROVIDER_NOT_CONFIGURED);
        }

        String redirectUri = oAuthProperties.resolveRedirectUri(provider);
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

    @Transactional
    public AuthLoginResponse loginWithCallback(SocialProvider provider, String code, String state) {
        if (!StringUtils.hasText(code)) {
            throw new ProjectException(AuthErrorCode.MISSING_AUTH_CODE);
        }

        validateState(provider, state);

        SocialUserProfile socialUserProfile = socialOAuthClient.getUserProfile(provider, code, state);
        UserService.UserCreationResult userCreationResult = userService.getOrCreateSocialUser(
                provider,
                socialUserProfile.providerUserId(),
                socialUserProfile.email(),
                socialUserProfile.nickname()
        );

        // getOrCreateSocialUser는 트랜잭션 밖에서 식별자만 반환한다.
        // 응답 생성 시 LAZY 필드를 읽어야 하므로 이 트랜잭션 안에서 managed 상태로 다시 조회한다.
        User user = userService.getUserById(userCreationResult.userId());
        AuthTokenService.AuthTokenPair tokenPair = authTokenService.issueTokens(user.getId());

        return AuthLoginResponse.of(
                user,
                tokenPair,
                userCreationResult.created(),
                resolveNextStep(user)
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
            throw new ProjectException(AuthErrorCode.INVALID_OAUTH_STATE);
        }
    }

    private AuthNextStep resolveNextStep(User user) {
        if (!user.isAgreementCompleted()) {
            return AuthNextStep.TERMS;
        }

        if (!user.isOnboardingResolved()) {
            return AuthNextStep.ONBOARDING;
        }

        return AuthNextStep.HOME;
    }
}
