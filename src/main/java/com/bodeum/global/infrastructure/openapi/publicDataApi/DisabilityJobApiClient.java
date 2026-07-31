package com.bodeum.global.infrastructure.openapi.publicDataApi;

import com.bodeum.domain.info.dto.response.publicData.DisabilityJobApiResponseDto;
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
public class DisabilityJobApiClient {

    private final RestClient restClient;
    private final String serviceKey;

    private static final String BASE_URL = "https://api.odcloud.kr/api/3072637/v1/uddi:6a9589d7-db1b-475b-b049-a957e834ed99";

    public DisabilityJobApiClient(RestClient.Builder restClientBuilder, @Value("${open-api.service-key}") String serviceKey) {
        this.restClient = restClientBuilder.build();
        this.serviceKey = serviceKey;
    }

    public List<DisabilityJobApiResponseDto.HeaderData> fetchAllData() {
        List<DisabilityJobApiResponseDto.HeaderData> allData = new ArrayList<>();
        int page = 1;
        int perPage = 300; // 효율적인 대량 수집 단위

        while (true) {
            URI uri = UriComponentsBuilder.fromUriString(BASE_URL)
                    .queryParam("page", page)
                    .queryParam("perPage", perPage)
                    .queryParam("serviceKey", serviceKey)
                    .build()
                    .encode()
                    .toUri();

            DisabilityJobApiResponseDto response;
            try {
                response = restClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(DisabilityJobApiResponseDto.class);
            } catch (Exception e) {
                log.error("[장애인 구인 정보 API Client] 외부 서버 연동 실패 - page: {}", page, e);
                throw new ProjectException(OpenApiErrorCode.EXTERNAL_SERVER_ERROR, e);
            }

            if (response == null || response.data() == null || response.data().isEmpty()) {
                if (page == 1) {
                    throw new ProjectException(OpenApiErrorCode.EMPTY_RESPONSE_DATA);
                }
                break;
            }

            allData.addAll(response.data());
            log.info("[장애인 구인 정보 API Client] page: {}, accumulated: {}/{}", page, allData.size(), response.totalCount());

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