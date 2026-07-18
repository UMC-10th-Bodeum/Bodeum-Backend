package com.bodeum.global.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bodeum.cors")
public class CorsProperties {

    /**
     * CORS 허용 출처. "https://*.vercel.app"처럼 와일드카드 패턴을 쓰므로
     * allowedOrigins가 아닌 allowedOriginPatterns로 적용한다.
     */
    private List<String> allowedOrigins = new ArrayList<>();
}
