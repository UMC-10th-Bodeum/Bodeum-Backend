package com.bodeum.global.infrastructure.openapi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bodeum.public-data.gg")
public class GgOpenApiProperties {

    private String baseUrl = "https://openapi.gg.go.kr";
    private String serviceKey = "";
}
