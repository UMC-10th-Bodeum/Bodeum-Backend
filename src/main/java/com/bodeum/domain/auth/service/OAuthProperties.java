package com.bodeum.domain.auth.service;

import com.bodeum.domain.auth.enumtype.SocialProvider;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bodeum.oauth")
public class OAuthProperties {

    private String baseUrl = "http://localhost:8080";

    /**
     * true면 client-id가 설정되지 않은 provider에 한해 개발용 모의 로그인을 허용한다.
     * 운영 환경에서는 반드시 false여야 한다.
     */
    private boolean mockEnabled = false;

    private ProviderRegistration kakao = new ProviderRegistration();
    private ProviderRegistration naver = new ProviderRegistration();

    private final Environment environment;

    public OAuthProperties() {
        this.environment = null;
    }

    @Autowired
    public OAuthProperties(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validateMockConfiguration() {
        if (mockEnabled && isProductionProfileActive()) {
            throw new IllegalStateException("OAuth mock login must be disabled in production profiles.");
        }
    }

    public ProviderRegistration getRegistration(SocialProvider provider) {
        return switch (provider) {
            case KAKAO -> kakao;
            case NAVER -> naver;
        };
    }

    public boolean isMockLoginAllowed() {
        return mockEnabled && !isProductionProfileActive();
    }

    public String resolveRedirectUri(SocialProvider provider) {
        ProviderRegistration registration = getRegistration(provider);
        if (registration != null && StringUtils.hasText(registration.getRedirectUri())) {
            return registration.getRedirectUri();
        }

        return baseUrl + "/api/v1/auth/callback/" + provider.getPath();
    }

    private boolean isProductionProfileActive() {
        if (environment == null) {
            return false;
        }

        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equalsIgnoreCase("prod") || profile.equalsIgnoreCase("production"));
    }

    @Getter
    @Setter
    public static class ProviderRegistration {

        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String scope;
        private String tokenUri;
        private String userInfoUri;

        public boolean isConfigured() {
            return clientId != null && !clientId.isBlank();
        }
    }
}
