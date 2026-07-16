package com.bodeum.global.infrastructure.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bodeum.aws.s3")
public class S3Properties {

    private String bucket;
    private String region;
    private String accessKey;
    private String secretKey;

    /**
     * 업로드된 객체에 접근하는 공개 기본 URL. 비어 있으면 표준 S3 가상 호스팅 URL을 사용한다.
     * CloudFront 등 CDN 앞단을 둘 경우 그 도메인을 지정한다.
     */
    private String publicBaseUrl;
}
