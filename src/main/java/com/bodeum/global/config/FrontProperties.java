package com.bodeum.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bodeum.front")
public class FrontProperties {

    /**
     * 소셜 로그인 완료 후 일회용 code를 붙여 리다이렉트할 프론트 콜백 URL.
     * 예: https://bodeum-site.vercel.app/auth/callback
     */
    private String callbackUrl;
}
