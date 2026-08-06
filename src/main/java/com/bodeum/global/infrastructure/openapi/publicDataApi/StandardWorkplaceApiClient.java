package com.bodeum.global.infrastructure.openapi.publicDataApi;

import com.bodeum.domain.info.dto.response.publicData.StandardWorkplaceApiResponseDto;
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
public class StandardWorkplaceApiClient {

    private final RestClient restClient;
    private final String serviceKey;

    private static final String BASE_URL = "https://api.odcloud.kr/api/3033670/v1/uddi:fe5b463e-0cc9-45a7-9c8d-ba96f76c1c2f";

    public StandardWorkplaceApiClient(RestClient.Builder restClientBuilder, @Value("${open-api.service-key}") String serviceKey) {
        this.restClient = restClientBuilder.build();
        this.serviceKey = serviceKey;
    }

    public List<StandardWorkplaceApiResponseDto.HeaderData> fetchAllData() {
        List<StandardWorkplaceApiResponseDto.HeaderData> allData = new ArrayList<>();
        int page = 1;
        int perPage = 200;

        while (true) {
            URI uri = UriComponentsBuilder.fromUriString(BASE_URL)
                    .queryParam("page", page)
                    .queryParam("perPage", perPage)
                    .queryParam("serviceKey", serviceKey)
                    .build()
                    .encode()
                    .toUri();

            StandardWorkplaceApiResponseDto response;
            try {
                response = restClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(StandardWorkplaceApiResponseDto.class);
            } catch (Exception e) {
                log.error("[장애인 표준사업장 API Client] 외부 서버 연동 실패 - page: {}", page, e);
                throw new ProjectException(OpenApiErrorCode.EXTERNAL_SERVER_ERROR, e);
            }

            if (response == null || response.data() == null || response.data().isEmpty()) {
                if (page == 1) {
                    throw new ProjectException(OpenApiErrorCode.EMPTY_RESPONSE_DATA);
                }
                break;
            }

            allData.addAll(response.data());
            log.info("[장애인 표준사업장 API Client] page: {}, accumulated: {}/{}", page, allData.size(), response.totalCount());

            if (response.totalCount() == null || allData.size() >= response.totalCount() || response.data().size() < perPage) {
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