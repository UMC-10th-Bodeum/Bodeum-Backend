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

    /** Redis를 인증 임시 데이터·refresh 세션 저장소로 사용할지 여부. */
    private boolean redisEnabled;

    /**
     * 매 요청 인증 시 사용자 조회 결과(principal)를 Redis에 캐시하는 시간.
     * DB가 원본이므로 stale을 짧게 유지하려고 기본값은 3분이다. redis-enabled=false면 무시된다.
     */
    private Duration principalCacheTtl = Duration.ofMinutes(3);
}
