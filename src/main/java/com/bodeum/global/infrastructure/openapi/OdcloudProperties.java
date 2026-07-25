package com.bodeum.global.infrastructure.openapi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bodeum.public-data.odcloud")
public class OdcloudProperties {

    private String baseUrl = "https://api.odcloud.kr/api";
    private String serviceKey = "";
}
