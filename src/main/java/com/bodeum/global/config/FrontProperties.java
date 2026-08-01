package com.bodeum.global.config;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bodeum.front")
public class FrontProperties {

    /**
     * 허용할 콜백 URL 최대 길이.
     * 이 값은 state에 실려 oauth_states.front_callback_url(VARCHAR(255))에 저장되므로,
     * 길이를 넘으면 저장 시점에 DataIntegrityViolationException이 나 로그인 시작이 500으로 실패한다.
     * 허용 origin이라도 path·query를 길게 붙이면 초과할 수 있어 여기서 먼저 막는다.
     */
    private static final int MAX_CALLBACK_URL_LENGTH = 255;

    /**
     * 소셜 로그인 완료 후 일회용 code를 붙여 리다이렉트할 기본 프론트 콜백 URL.
     * 예: https://bodeum-site.vercel.app/auth/callback
     */
    private String callbackUrl;

    /**
     * 프론트가 로그인 시작 시 직접 지정할 수 있는 콜백 URL의 허용 origin 목록.
     * 로컬 개발 서버는 팀원마다 포트가 달라 "http://localhost:*" 같은 와일드카드가 필요하므로
     * CORS와 동일한 패턴 매칭(allowedOriginPatterns)을 재사용한다.
     */
    private List<String> allowedCallbackOrigins = new ArrayList<>();

    // @PostConstruct에서만 채우는 내부 캐시라, 클래스 레벨 @Getter/@Setter의 공개 API에서 제외한다.
    // setter가 열려 있으면 bodeum.front.callback-origin-matcher 같은 키에 바인딩될 수도 있다.
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private CorsConfiguration callbackOriginMatcher;

    @PostConstruct
    void initCallbackOriginMatcher() {
        callbackOriginMatcher = new CorsConfiguration();
        callbackOriginMatcher.setAllowedOriginPatterns(allowedCallbackOrigins);
    }

    /**
     * 프론트가 요청한 콜백 URL이 허용 origin이면 그대로, 아니면 기본 콜백 URL을 돌려준다.
     * 허용되지 않은 값을 예외로 막지 않고 기본값으로 폴백하는 이유는, 오픈 리다이렉트는 폴백만으로
     * 이미 차단되고 잘못된 파라미터 하나가 로그인 자체를 막지 않게 하기 위해서다.
     */
    public String resolveCallbackUrl(String requestedCallbackUrl) {
        if (!StringUtils.hasText(requestedCallbackUrl)
                || requestedCallbackUrl.length() > MAX_CALLBACK_URL_LENGTH
                || !isAllowedCallbackUrl(requestedCallbackUrl)) {
            return callbackUrl;
        }

        return requestedCallbackUrl;
    }

    private boolean isAllowedCallbackUrl(String requestedCallbackUrl) {
        URI uri;
        try {
            uri = new URI(requestedCallbackUrl);
        } catch (URISyntaxException e) {
            return false;
        }

        // userInfo가 있으면 "http://localhost:5173@evil.com"처럼 호스트를 착각하게 만드는 형태라 거부한다.
        if (uri.getScheme() == null || uri.getHost() == null || uri.getUserInfo() != null) {
            return false;
        }

        String origin = uri.getPort() == -1
                ? uri.getScheme() + "://" + uri.getHost()
                : uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort();

        return callbackOriginMatcher.checkOrigin(origin) != null;
    }
}
