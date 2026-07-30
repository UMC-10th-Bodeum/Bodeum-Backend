package com.bodeum.global.infrastructure.openapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GgOpenApiClient {

    private static final String SUCCESS_CODE = "INFO-000";
    private static final String NO_DATA_CODE = "INFO-200";

    private final RestClient restClient;
    private final GgOpenApiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GgOpenApiClient(
            RestClient.Builder restClientBuilder,
            GgOpenApiProperties properties
    ) {
        this.restClient = restClientBuilder.clone()
                .baseUrl(properties.getBaseUrl())
                .build();
        this.properties = properties;
    }

    public GgOpenApiPageResponse fetchPage(
            String resourcePath,
            String responseKey,
            int page,
            int pageSize
    ) {
        validateRequest(resourcePath, responseKey, page, pageSize);

        try {
            String responseBody = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(resourcePath)
                            .queryParam("KEY", "{serviceKey}")
                            .queryParam("Type", "json")
                            .queryParam("pIndex", page)
                            .queryParam("pSize", pageSize)
                            .build(properties.getServiceKey()))
                    .retrieve()
                    .body(String.class);

            if (!StringUtils.hasText(responseBody)) {
                throw new PublicDataClientException("경기데이터드림 API가 빈 응답을 반환했습니다.");
            }
            return parseResponse(responseBody, responseKey);
        } catch (PublicDataClientException e) {
            throw e;
        } catch (RestClientException e) {
            throw new PublicDataClientException("경기데이터드림 API 호출에 실패했습니다.", e);
        }
    }

    private GgOpenApiPageResponse parseResponse(String responseBody, String responseKey) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            validateResult(root.path("RESULT"));

            JsonNode sections = root.path(responseKey);
            if (!sections.isArray() || sections.isEmpty()) {
                throw new PublicDataClientException("경기데이터드림 API 응답 구조가 올바르지 않습니다.");
            }

            JsonNode head = findSection(sections, "head");
            validateHead(head);

            int totalCount = extractTotalCount(head);
            JsonNode rowNode = findSection(sections, "row");
            List<Map<String, Object>> rows = rowNode.isArray()
                    ? objectMapper.convertValue(
                            rowNode,
                            new TypeReference<List<Map<String, Object>>>() {
                            }
                    )
                    : List.of();
            return new GgOpenApiPageResponse(totalCount, rows);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new PublicDataClientException("경기데이터드림 API 응답을 해석하지 못했습니다.", e);
        }
    }

    private JsonNode findSection(JsonNode sections, String fieldName) {
        for (JsonNode section : sections) {
            JsonNode value = section.path(fieldName);
            if (!value.isMissingNode()) {
                return value;
            }
        }
        return objectMapper.missingNode();
    }

    private void validateHead(JsonNode head) {
        if (!head.isArray()) {
            return;
        }
        for (JsonNode item : head) {
            validateResult(item.path("RESULT"));
        }
    }

    private void validateResult(JsonNode result) {
        if (!result.isObject()) {
            return;
        }

        String code = result.path("CODE").asText();
        if (!StringUtils.hasText(code) || SUCCESS_CODE.equals(code) || NO_DATA_CODE.equals(code)) {
            return;
        }

        String message = result.path("MESSAGE").asText("알 수 없는 오류");
        throw new PublicDataClientException(
                "경기데이터드림 API 오류: " + code + " (" + message + ")"
        );
    }

    private int extractTotalCount(JsonNode head) {
        if (!head.isArray()) {
            return 0;
        }
        for (JsonNode item : head) {
            JsonNode totalCount = item.path("list_total_count");
            if (totalCount.canConvertToInt()) {
                return totalCount.asInt();
            }
        }
        return 0;
    }

    private void validateRequest(String resourcePath, String responseKey, int page, int pageSize) {
        if (!StringUtils.hasText(properties.getServiceKey())) {
            throw new PublicDataClientException("경기데이터드림 인증키가 설정되지 않았습니다.");
        }
        if (!StringUtils.hasText(resourcePath) || !resourcePath.startsWith("/")) {
            throw new IllegalArgumentException("경기데이터드림 resourcePath는 /로 시작해야 합니다.");
        }
        if (!StringUtils.hasText(responseKey)) {
            throw new IllegalArgumentException("경기데이터드림 responseKey는 필수입니다.");
        }
        if (page < 1 || pageSize < 1) {
            throw new IllegalArgumentException("경기데이터드림 page와 pageSize는 1 이상이어야 합니다.");
        }
    }
}
