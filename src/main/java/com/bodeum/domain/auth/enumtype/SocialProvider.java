package com.bodeum.domain.auth.enumtype;

import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.Arrays;

public enum SocialProvider {

    KAKAO(
            "kakao",
            "카카오",
            "https://kauth.kakao.com/oauth/authorize",
            "https://kauth.kakao.com/oauth/token",
            "https://kapi.kakao.com/v2/user/me",
            // 동의항목 미설정(정보 미수집) 상태이므로 scope를 요청하지 않는다. id는 scope 없이도 내려온다.
            null,
            false
    ),
    NAVER(
            "naver",
            "네이버",
            "https://nid.naver.com/oauth2.0/authorize",
            "https://nid.naver.com/oauth2.0/token",
            "https://openapi.naver.com/v1/nid/me",
            null,
            true
    );

    private final String path;
    private final String displayName;
    private final String authorizationUri;
    private final String tokenUri;
    private final String userInfoUri;
    private final String defaultScope;
    private final boolean clientSecretRequired;

    SocialProvider(
            String path,
            String displayName,
            String authorizationUri,
            String tokenUri,
            String userInfoUri,
            String defaultScope,
            boolean clientSecretRequired
    ) {
        this.path = path;
        this.displayName = displayName;
        this.authorizationUri = authorizationUri;
        this.tokenUri = tokenUri;
        this.userInfoUri = userInfoUri;
        this.defaultScope = defaultScope;
        this.clientSecretRequired = clientSecretRequired;
    }

    public String getPath() {
        return path;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAuthorizationUri() {
        return authorizationUri;
    }

    public String getTokenUri() {
        return tokenUri;
    }

    public String getUserInfoUri() {
        return userInfoUri;
    }

    public String getDefaultScope() {
        return defaultScope;
    }

    public boolean isClientSecretRequired() {
        return clientSecretRequired;
    }

    public static SocialProvider from(String value) {
        return Arrays.stream(values())
                .filter(provider -> provider.path.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.BAD_REQUEST));
    }
}
