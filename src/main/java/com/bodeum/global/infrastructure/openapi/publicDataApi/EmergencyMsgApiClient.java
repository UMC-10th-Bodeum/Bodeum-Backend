package com.bodeum.global.infrastructure.openapi.publicDataApi;

import com.bodeum.domain.info.dto.response.publicData.EmergencyMsgApiResponseDto;
import com.bodeum.global.apiPayload.code.OpenApiErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class EmergencyMsgApiClient {

    private final RestClient restClient;
    private final String serviceKey;

    private static final String BASE_URL = "http://apis.data.go.kr/B552657/ErmctInfoInqireService/getEmrrmSrsillDissMsgInqire";

    public EmergencyMsgApiClient(RestClient.Builder restClientBuilder, @Value("${open-api.service-key}") String serviceKey) {
        this.restClient = restClientBuilder.build();
        this.serviceKey = serviceKey;
    }

    /**
     * 공공데이터 API 자체에서 STAGE1, STAGE2 필터링을 지원하지 않으므로
     * 조건 없이 전량 데이터를 수집합니다.
     */
    public List<EmergencyMsgApiResponseDto.Item> fetchAllData() {
        List<EmergencyMsgApiResponseDto.Item> allData = new ArrayList<>();
        int pageNo = 1;
        int numOfRows = 100;

        while (true) {
            URI uri = UriComponentsBuilder.fromUriString(BASE_URL)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("pageNo", pageNo)
                    .queryParam("numOfRows", numOfRows)
                    .queryParam("_type", "json")
                    .build()
                    .encode()
                    .toUri();

            EmergencyMsgApiResponseDto response;
            try {
                response = restClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(EmergencyMsgApiResponseDto.class);
            } catch (Exception e) {
                log.error("[응급실 메시지 API Client] 외부 서버 연동 실패 - page: {}", pageNo, e);
                throw new ProjectException(OpenApiErrorCode.EXTERNAL_SERVER_ERROR, e);
            }

            List<EmergencyMsgApiResponseDto.Item> items = extractItems(response);

            if (items.isEmpty()) {
                break;
            }

            allData.addAll(items);
            Integer totalCount = extractTotalCount(response);

            log.info("[응급실 메시지 API Client] page: {}, accumulated: {}/{}", pageNo, allData.size(), totalCount);

            if (totalCount == null || allData.size() >= totalCount || items.size() < numOfRows) {
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

    private List<EmergencyMsgApiResponseDto.Item> extractItems(EmergencyMsgApiResponseDto response) {
        if (response == null || response.response() == null || response.response().body() == null) {
            return Collections.emptyList();
        }
        var body = response.response().body();
        if (body.items() == null || body.items().item() == null) {
            return Collections.emptyList();
        }
        return body.items().item();
    }

    private Integer extractTotalCount(EmergencyMsgApiResponseDto response) {
        if (response == null || response.response() == null || response.response().body() == null) {
            return 0;
        }
        return response.response().body().totalCount();
    }
}