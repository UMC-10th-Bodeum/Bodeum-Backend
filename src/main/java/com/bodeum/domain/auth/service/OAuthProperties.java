package com.bodeum.domain.auth.service;

import com.bodeum.domain.auth.enumtype.SocialProvider;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
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

    private static final Set<String> MOCK_ALLOWED_PROFILES = Set.of("local", "dev", "test");

    private String baseUrl = "http://localhost:8080";

    /**
     * true면 client-id가 설정되지 않은 provider에 한해 개발용 모의 로그인을 허용한다.
     * local/dev/test 프로필에서만 활성화할 수 있다.
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
            throw new IllegalStateException("OAuth mock login is only allowed in local, dev, or test profiles.");
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
            return true;
        }

        return Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .noneMatch(MOCK_ALLOWED_PROFILES::contains);
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
