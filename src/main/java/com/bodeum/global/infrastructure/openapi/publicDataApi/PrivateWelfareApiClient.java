package com.bodeum.global.infrastructure.openapi.publicDataApi;

import com.bodeum.domain.info.dto.response.publicData.PrivateWelfareApiResponseDto;
import com.bodeum.global.apiPayload.code.OpenApiErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class PrivateWelfareApiClient {

    private final RestClient restClient;
    private final String serviceKey;

    private static final String BASE_URL = "https://api.odcloud.kr/api/15116392/v1/uddi:44e91fb3-7ca8-4f83-a978-d42109ed8443";

    public PrivateWelfareApiClient(RestClient.Builder restClientBuilder, @Value("${open-api.service-key}") String serviceKey) {
        this.restClient = restClientBuilder.build();
        this.serviceKey = serviceKey;
    }

    public List<PrivateWelfareApiResponseDto.HeaderData> fetchAllData() {
        List<PrivateWelfareApiResponseDto.HeaderData> allData = new ArrayList<>();
        int page = 1;
        int perPage = 100;

        while (true) {
            URI uri = UriComponentsBuilder.fromUriString(BASE_URL)
                    .queryParam("page", page)
                    .queryParam("perPage", perPage)
                    .queryParam("serviceKey", serviceKey)
                    .build()
                    .encode()
                    .toUri();

            PrivateWelfareApiResponseDto response;
            try {
                response = restClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(PrivateWelfareApiResponseDto.class);
            } catch (Exception e) {
                log.error("[민간 복지 서비스 API Client] 외부 서버 연동 실패 - page: {}", page, e);
                throw new ProjectException(OpenApiErrorCode.EXTERNAL_SERVER_ERROR, e);
            }

            if (response == null || response.data() == null || response.data().isEmpty()) {
                if (page == 1) {
                    throw new ProjectException(OpenApiErrorCode.EMPTY_RESPONSE_DATA);
                }
                break;
            }

            allData.addAll(response.data());
            log.info("[민간 복지 서비스 API Client] page: {}, accumulated: {}/{}", page, allData.size(), response.totalCount());

            if (allData.size() >= response.totalCount() || response.data().size() < perPage) {
                break;
            }

            page++;

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return allData;
    }
}