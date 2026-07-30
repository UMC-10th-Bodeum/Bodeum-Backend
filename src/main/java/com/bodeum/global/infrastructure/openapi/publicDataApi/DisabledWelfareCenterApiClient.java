package com.bodeum.global.infrastructure.openapi.publicDataApi;

import com.bodeum.domain.info.dto.response.publicData.DisabledWelfareCenterApiResponseDto;
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
public class DisabledWelfareCenterApiClient {

    private final RestClient restClient;
    private final String serviceKey;

    private static final String BASE_URL = "https://api.odcloud.kr/api/15075529/v1/uddi:f153fd90-c36c-44d5-a9f1-8a0041cfa9b7";

    public DisabledWelfareCenterApiClient(RestClient.Builder restClientBuilder, @Value("${open-api.service-key}") String serviceKey) {
        this.restClient = restClientBuilder.build();
        this.serviceKey = serviceKey;
    }

    public List<DisabledWelfareCenterApiResponseDto.HeaderData> fetchAllData() {
        List<DisabledWelfareCenterApiResponseDto.HeaderData> allData = new ArrayList<>();
        int page = 1;
        int perPage = 500;

        while (true) {
            URI uri = UriComponentsBuilder.fromUriString(BASE_URL)
                    .queryParam("page", page)
                    .queryParam("perPage", perPage)
                    .queryParam("serviceKey", serviceKey)
                    .build()
                    .encode()
                    .toUri();

            DisabledWelfareCenterApiResponseDto response;
            try {
                response = restClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(DisabledWelfareCenterApiResponseDto.class);
            } catch (Exception e) {
                log.error("[장애인 복지관 API Client] 외부 서버 연동 실패 - page: {}", page, e);
                throw new ProjectException(OpenApiErrorCode.EXTERNAL_SERVER_ERROR, e);
            }

            if (response == null || response.data() == null) {
                if (page == 1) {
                    throw new ProjectException(OpenApiErrorCode.EMPTY_RESPONSE_DATA);
                }
                break;
            }

            if (response.data().isEmpty()) {
                break;
            }

            allData.addAll(response.data());
            log.info("[장애인 복지관 API Client] page: {}, accumulated: {}/{}", page, allData.size(), response.totalCount());

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

        if (allData.isEmpty()) {
            throw new ProjectException(OpenApiErrorCode.EMPTY_RESPONSE_DATA);
        }

        return allData;
    }
}