package com.bodeum.global.infrastructure.storage;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

    /**
     * 기동 시점에 S3 설정을 검증한다.
     * 검증하지 않으면 설정 누락이 첫 업로드 요청에서야 드러나, 잘못 뜬 인스턴스가
     * 배포 성공으로 처리된다.
     */
    @PostConstruct
    public void validate() {
        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException("S3 버킷이 설정되지 않았다. AWS_S3_BUCKET을 확인할 것.");
        }

        if (!StringUtils.hasText(region)) {
            throw new IllegalStateException("S3 리전이 설정되지 않았다. AWS_S3_REGION을 확인할 것.");
        }

        // 둘 다 비어 있으면 DefaultCredentialsProvider(EC2 인스턴스 프로파일 등)를 쓴다.
        // 하나만 설정되면 의도와 달리 조용히 그쪽으로 넘어가 권한 오류로만 드러나므로 기동을 막는다.
        boolean hasAccessKey = StringUtils.hasText(accessKey);
        boolean hasSecretKey = StringUtils.hasText(secretKey);
        if (hasAccessKey != hasSecretKey) {
            throw new IllegalStateException(
                    "AWS_ACCESS_KEY와 AWS_SECRET_KEY는 둘 다 설정하거나 둘 다 비워야 한다.");
        }
    }
}
