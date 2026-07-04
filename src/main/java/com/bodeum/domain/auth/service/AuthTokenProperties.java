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
     * 비어 있으면 기동 시 임시 키를 생성한다. (재시작하면 기존 access token 전부 무효화)
     */
    private String jwtSecret;

    private Duration accessTokenTtl = Duration.ofHours(1);
    private Duration refreshTokenTtl = Duration.ofDays(14);
}
