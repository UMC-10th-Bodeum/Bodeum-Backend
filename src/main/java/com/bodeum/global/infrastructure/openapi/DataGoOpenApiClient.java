package com.bodeum.global.infrastructure.openapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class DataGoOpenApiClient {

    private static final String SUCCESS_CODE = "00";
    private static final String NO_DATA_CODE = "03";
    private static final int MAX_PAGE_SIZE = 1000;

    private final RestClient restClient;
    private final DataGoOpenApiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public DataGoOpenApiClient(
            RestClient.Builder restClientBuilder,
            DataGoOpenApiProperties properties
    ) {
        this(restClientBuilder, properties, true);
    }

    DataGoOpenApiClient(
            RestClient.Builder restClientBuilder,
            DataGoOpenApiProperties properties,
            boolean applyTimeouts
    ) {
        RestClient.Builder builder = restClientBuilder.clone()
                .baseUrl(properties.getBaseUrl());
        if (applyTimeouts) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(properties.getConnectTimeout());
            requestFactory.setReadTimeout(properties.getReadTimeout());
            builder.requestFactory(requestFactory);
        }
        this.restClient = builder.build();
        this.properties = properties;
    }

    public DataGoOpenApiPageResponse fetchPage(String resourcePath, int page, int pageSize) {
        validateRequest(resourcePath, page, pageSize);

        try {
            String responseBody = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(resourcePath)
                            .queryParam("serviceKey", properties.getServiceKey())
                            .queryParam("pageNo", page)
                            .queryParam("numOfRows", pageSize)
                            .queryParam("type", "json")
                            .build())
                    .retrieve()
                    .body(String.class);

            if (!StringUtils.hasText(responseBody)) {
                throw new PublicDataClientException("공공데이터포털 API가 빈 응답을 반환했습니다.");
            }
            return parseResponse(responseBody);
        } catch (PublicDataClientException e) {
            throw e;
        } catch (RestClientException e) {
            throw new PublicDataClientException("공공데이터포털 API 호출에 실패했습니다.", e);
        }
    }

    private DataGoOpenApiPageResponse parseResponse(String responseBody) {
        try {
            JsonNode response = objectMapper.readTree(responseBody).path("response");
            if (!response.isObject()) {
                throw new PublicDataClientException("공공데이터포털 API 응답 구조가 올바르지 않습니다.");
            }

            JsonNode header = response.path("header");
            String resultCode = header.path("resultCode").asText();
            String resultMessage = header.path("resultMsg").asText("알 수 없는 오류");
            if (NO_DATA_CODE.equals(resultCode)) {
                return new DataGoOpenApiPageResponse(0, List.of());
            }
            if (!SUCCESS_CODE.equals(resultCode)) {
                throw new PublicDataClientException(
                        "공공데이터포털 API 오류: " + resultCode + " (" + resultMessage + ")"
                );
            }

            JsonNode body = response.path("body");
            if (!body.isObject()) {
                throw new PublicDataClientException("공공데이터포털 API 본문이 올바르지 않습니다.");
            }

            int totalCount = parseInteger(body.path("totalCount"));
            return new DataGoOpenApiPageResponse(totalCount, extractRows(body.path("items")));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new PublicDataClientException("공공데이터포털 API 응답을 해석하지 못했습니다.", e);
        }
    }

    private List<Map<String, Object>> extractRows(JsonNode items) {
        JsonNode rows = items.isObject() && items.has("item") ? items.path("item") : items;
        if (rows.isMissingNode() || rows.isNull()) {
            return List.of();
        }
        if (rows.isArray()) {
            return objectMapper.convertValue(
                    rows,
                    new TypeReference<List<Map<String, Object>>>() {
                    }
            );
        }
        if (rows.isObject()) {
            return List.of(objectMapper.convertValue(
                    rows,
                    new TypeReference<Map<String, Object>>() {
                    }
            ));
        }
        throw new PublicDataClientException("공공데이터포털 API 목록 형식이 올바르지 않습니다.");
    }

    private int parseInteger(JsonNode node) {
        if (node.canConvertToInt()) {
            return node.asInt();
        }
        String value = node.asText();
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        return Integer.parseInt(value);
    }

    private void validateRequest(String resourcePath, int page, int pageSize) {
        if (!StringUtils.hasText(properties.getServiceKey())) {
            throw new PublicDataClientException("공공데이터포털 인증키가 설정되지 않았습니다.");
        }
        if (!StringUtils.hasText(resourcePath) || !resourcePath.startsWith("/")) {
            throw new IllegalArgumentException("공공데이터포털 resourcePath는 /로 시작해야 합니다.");
        }
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("page는 1 이상, pageSize는 1 이상 1000 이하여야 합니다.");
        }
    }
}
