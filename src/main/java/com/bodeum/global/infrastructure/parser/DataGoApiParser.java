package com.bodeum.global.infrastructure.parser;

import com.bodeum.global.infrastructure.dto.DataGoApiResponse;
import com.bodeum.global.infrastructure.dto.ExtractedDataResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

// 데이터고포털(JSON 및 XML 변환본)의 계층 구조를 파싱하는 구현체.

@Component
public class DataGoApiParser implements ApiParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(String urlType) {
        return "DATAGO".equals(urlType);
    }

    @Override
    public ExtractedDataResult parse(Map<String, Object> rawResponse, String apiKey) {
        DataGoApiResponse response = objectMapper.convertValue(rawResponse, DataGoApiResponse.class);

        // 계층 구조가 비어있지 않은지 단계별로 안전하게 확인.
        if (response == null || response.response() == null || response.response().body() == null) {
            return new ExtractedDataResult(0, List.of());
        }

        // body 로 총 개수 카운트.
        DataGoApiResponse.Body body = response.response().body();
        int totalCount = body.totalCount();

        // 구조가 깊음 .. body -> items -> item (실제 데이터)
        List<Map<String, Object>> records = List.of();
        if (body.items() != null && body.items().item() != null) {
            records = body.items().item();
        }

        return new ExtractedDataResult(totalCount, records);
    }
}