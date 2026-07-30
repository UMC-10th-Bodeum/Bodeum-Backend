package com.bodeum.global.infrastructure.openapi;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OdcloudClient {

    private final RestClient restClient;
    private final OdcloudProperties properties;

    public OdcloudClient(RestClient.Builder restClientBuilder, OdcloudProperties properties) {
        this.restClient = restClientBuilder.clone()
                .baseUrl(properties.getBaseUrl())
                .build();
        this.properties = properties;
    }

    public OdcloudPageResponse fetchPage(String resourcePath, int page, int perPage) {
        validateRequest(resourcePath, page, perPage);

        try {
            OdcloudPageResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(resourcePath)
                            .queryParam("page", page)
                            .queryParam("perPage", perPage)
                            .queryParam("serviceKey", "{serviceKey}")
                            .build(properties.getServiceKey()))
                    .retrieve()
                    .body(OdcloudPageResponse.class);

            if (response == null) {
                throw new PublicDataClientException("ODCloud API가 빈 응답을 반환했습니다.");
            }
            return response;
        } catch (PublicDataClientException e) {
            throw e;
        } catch (RestClientException e) {
            throw new PublicDataClientException("ODCloud API 호출에 실패했습니다.", e);
        }
    }

    private void validateRequest(String resourcePath, int page, int perPage) {
        if (!StringUtils.hasText(properties.getServiceKey())) {
            throw new PublicDataClientException("ODCloud 인증키가 설정되지 않았습니다.");
        }
        if (!StringUtils.hasText(resourcePath) || !resourcePath.startsWith("/")) {
            throw new IllegalArgumentException("ODCloud resourcePath는 /로 시작해야 합니다.");
        }
        if (page < 1 || perPage < 1) {
            throw new IllegalArgumentException("ODCloud page와 perPage는 1 이상이어야 합니다.");
        }
    }
}
