package com.bodeum.domain.auth.service;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bodeum.auth")
public class AuthTokenProperties {

    /**
     * HS256 서명 키. 32바이트(256비트) 이상이어야 한다.
     * 비어 있으면 애플리케이션 기동을 중단한다.
     */
    private String jwtSecret;

    private Duration accessTokenTtl = Duration.ofHours(1);
    private Duration refreshTokenTtl = Duration.ofDays(14);

    /**
     * 소셜 로그인 콜백이 프론트로 넘기는 일회용 로그인 code의 유효 시간.
     * 프론트가 code를 교환(exchange)할 때까지의 짧은 핸드오프 창이라 기본값은 60초다.
     */
    private Duration loginCodeTtl = Duration.ofSeconds(60);
}
