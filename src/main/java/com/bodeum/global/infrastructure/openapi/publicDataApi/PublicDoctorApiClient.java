package com.bodeum.global.infrastructure.openapi.publicDataApi;

import com.bodeum.domain.info.dto.response.publicData.PublicDoctorApiResponseDto;
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
public class PublicDoctorApiClient {

    private final RestClient restClient;
    private final String serviceKey;

    private static final String BASE_URL = "https://api.odcloud.kr/api/15144843/v1/uddi:76b1c743-cad2-4363-93cb-f13d79b00d0a";

    public PublicDoctorApiClient(RestClient.Builder restClientBuilder, @Value("${open-api.service-key}") String serviceKey) {
        this.restClient = restClientBuilder.build();
        this.serviceKey = serviceKey;
    }

    public List<PublicDoctorApiResponseDto.HeaderData> fetchAllData() {
        List<PublicDoctorApiResponseDto.HeaderData> allData = new ArrayList<>();
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

            PublicDoctorApiResponseDto response;
            try {
                response = restClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(PublicDoctorApiResponseDto.class);
            } catch (Exception e) {
                log.error("[장애인 건강주치의 API Client] 외부 서버 연동 실패 - page: {}", page, e);
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
            log.info("[장애인 건강주치의 API Client] page: {}, accumulated: {}/{}", page, allData.size(), response.totalCount());

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