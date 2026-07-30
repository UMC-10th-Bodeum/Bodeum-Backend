package com.bodeum.global.infrastructure.openapi.publicDataApi;

import com.bodeum.domain.info.dto.response.publicData.NationalWelfareApiResponseDto;
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
public class NationalWelfareApiClient {

    private final RestClient restClient;
    private final String serviceKey;

    private static final String BASE_URL = "https://apis.data.go.kr/B554287/NationalWelfareInformationsV001/NationalWelfarelistV001";

    public NationalWelfareApiClient(RestClient.Builder restClientBuilder, @Value("${open-api.service-key}") String serviceKey) {
        this.restClient = restClientBuilder.build();
        this.serviceKey = serviceKey;
    }

    public List<NationalWelfareApiResponseDto.ServList> fetchAllData() {
        List<NationalWelfareApiResponseDto.ServList> allData = new ArrayList<>();
        int pageNo = 1;
        int numOfRows = 100;

        while (true) {
            URI uri = UriComponentsBuilder.fromUriString(BASE_URL)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("callTp", "D")
                    .queryParam("srchKeyCode", "003") // 장애인 관련 복지 서비스 키코드
                    .queryParam("pageNo", pageNo)
                    .queryParam("numOfRows", numOfRows)
                    .build()
                    .encode()
                    .toUri();

            NationalWelfareApiResponseDto response;
            try {
                response = restClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(NationalWelfareApiResponseDto.class);
            } catch (Exception e) {
                log.error("[국가 복지 서비스 API Client] 외부 서버 연동 실패 - pageNo: {}", pageNo, e);
                throw new ProjectException(OpenApiErrorCode.EXTERNAL_SERVER_ERROR, e);
            }

            if (response == null || response.servList() == null || response.servList().isEmpty()) {
                if (pageNo == 1) {
                    throw new ProjectException(OpenApiErrorCode.EMPTY_RESPONSE_DATA);
                }
                break;
            }

            allData.addAll(response.servList());
            log.info("[국가 복지 서비스 API Client] pageNo: {}, accumulated: {}/{}", pageNo, allData.size(), response.totalCount());

            if (response.totalCount() == null || allData.size() >= response.totalCount() || response.servList().size() < numOfRows) {
                break;
            }

            pageNo++;

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return allData;
    }
}