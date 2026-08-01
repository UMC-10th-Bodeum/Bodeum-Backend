package com.bodeum.domain.auth.service;

import com.bodeum.domain.auth.dto.response.AuthLoginResponse;
import com.bodeum.domain.auth.dto.response.AuthTokenResponse;
import com.bodeum.domain.auth.enums.AuthNextStep;
import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.auth.exception.AuthErrorCode;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.service.UserService;
import com.bodeum.global.apiPayload.code.BaseErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import com.bodeum.global.config.FrontProperties;
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
    private final AuthLoginCodeStore authLoginCodeStore;
    private final FrontProperties frontProperties;

    public URI createLoginRedirectUri(SocialProvider provider, String requestedFrontCallbackUrl) {
        OAuthProperties.ProviderRegistration registration = oAuthProperties.getRegistration(provider);
        if (registration == null || !registration.isConfigured()) {
            throw new ProjectException(AuthErrorCode.PROVIDER_NOT_CONFIGURED);
        }

        String redirectUri = oAuthProperties.resolveRedirectUri(provider);
        String scope = StringUtils.hasText(registration.getScope())
                ? registration.getScope()
                : provider.getDefaultScope();

        // 소셜 제공자에게 넘기는 redirect_uri는 콘솔 등록값이라 항상 고정이고,
        // 요청마다 달라지는 것은 그 뒤에 이어지는 "우리 서버 → 프론트" 구간뿐이다.
        String frontCallbackUrl = frontProperties.resolveCallbackUrl(requestedFrontCallbackUrl);
        if (StringUtils.hasText(requestedFrontCallbackUrl)
                && !requestedFrontCallbackUrl.equals(frontCallbackUrl)) {
            log.warn("[AUTH] 허용되지 않은 프론트 콜백 URL 요청 provider={} requested={}",
                    provider, sanitizeForLog(requestedFrontCallbackUrl));
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(provider.getAuthorizationUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", registration.getClientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", oAuthStateStore.issue(provider, frontCallbackUrl));

        if (StringUtils.hasText(scope)) {
            builder.queryParam("scope", scope);
        }

        return builder.encode().build().toUri();
    }

    /**
     * 로그인 시작 시 state에 실어 둔 프론트 콜백 URL을 돌려준다.
     * state가 없거나 값이 비어 있으면 서버 기본 콜백 URL을 쓴다.
     */
    public String resolveFrontCallbackUrl(String state) {
        return oAuthStateStore.findFrontCallbackUrl(state)
                .orElseGet(frontProperties::getCallbackUrl);
    }

    @Transactional
    public URI loginWithCallback(SocialProvider provider, String code, String state, String frontCallbackUrl) {
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

        // 콜백은 브라우저 전체 리다이렉트라 응답 body를 프론트가 받을 수 없다.
        // 따라서 토큰을 여기서 발급하지 않고 일회용 code만 발급해 프론트 콜백 URL로 넘긴다.
        // 실제 토큰은 프론트가 code를 교환(exchange)할 때 발급한다(토큰이 URL/로그/히스토리에 남지 않도록).
        String loginCode = authLoginCodeStore.issue(userCreationResult.userId(), userCreationResult.created());

        return buildFrontRedirectUri(frontCallbackUrl, loginCode);
    }

    @Transactional
    public AuthLoginResponse exchange(String oneTimeCode) {
        AuthLoginCodeStore.Consumed consumed = authLoginCodeStore.consume(oneTimeCode);

        // 응답 생성 시 LAZY 필드를 읽어야 하므로 이 트랜잭션 안에서 managed 상태로 조회한다.
        User user = userService.getUserById(consumed.userId());
        AuthTokenService.AuthTokenPair tokenPair = authTokenService.issueTokens(user.getId());

        return AuthLoginResponse.of(
                user,
                tokenPair,
                consumed.isNewUser(),
                resolveNextStep(user)
        );
    }

    public AuthTokenResponse refresh(String refreshToken) {
        return AuthTokenResponse.from(authTokenService.refresh(refreshToken));
    }

    public void logout(Long userId, String refreshToken, String accessToken) {
        // refresh는 전달된 토큰의 session-family만 먼저 폐기한다. 다른 기기 family는 유지한다.
        // refresh가 먼저 회전했다면 family 폐기가 새 토큰까지 제거하고, logout이 먼저라면
        // revoked-family marker가 뒤늦은 회전 저장을 거부한다.
        authTokenService.revoke(userId, refreshToken);
        // 현재 요청에 사용한 access token만 폐기한다. 같은 사용자의 다른 기기 토큰에는 영향이 없다.
        authTokenService.revokeAccessToken(accessToken);
    }

    private URI buildFrontRedirectUri(String frontCallbackUrl, String loginCode) {
        // code는 60초·1회용이라 교환 즉시 폐기되므로 쿼리 파라미터로 전달해도 안전하다.
        return buildFrontRedirectUri(frontCallbackUrl, "code", loginCode);
    }

    /**
     * 콜백 실패 시에도 브라우저 전체 네비게이션이라 JSON 대신 프론트 콜백 URL로 리다이렉트한다.
     * 프론트는 code 없이 error가 오면 로그인 화면으로 안내한다.
     */
    public URI buildFrontErrorRedirectUri(BaseErrorCode errorCode, String frontCallbackUrl) {
        return buildFrontRedirectUri(frontCallbackUrl, "error", errorCode.getCode());
    }

    private URI buildFrontRedirectUri(String frontCallbackUrl, String paramName, String paramValue) {
        return UriComponentsBuilder.fromUriString(frontCallbackUrl)
                .queryParam(paramName, paramValue)
                .encode()
                .build()
                .toUri();
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

    /**
     * 쿼리 파라미터로 들어온 값이라 개행·제어 문자가 섞이면 가짜 로그 라인을 끼워 넣을 수 있다.
     * 원인 파악에는 값 자체가 필요하므로 버리지 않고 제어 문자만 치환해 남긴다.
     */
    private String sanitizeForLog(String value) {
        return value.replaceAll("\\p{Cntrl}", "_");
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
