package com.bodeum.global.infrastructure.openapi;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bodeum.public-data.datago")
public class DataGoOpenApiProperties {

    private String baseUrl = "https://api.data.go.kr/openapi";
    private String serviceKey = "";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(30);
}
