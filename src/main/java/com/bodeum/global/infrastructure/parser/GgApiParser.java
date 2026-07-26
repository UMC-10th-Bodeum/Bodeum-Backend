package com.bodeum.global.infrastructure.parser;

import com.bodeum.global.infrastructure.dto.ExtractedDataResult;
import com.bodeum.global.infrastructure.dto.GgApiResponse;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

// 경기도 오픈데이터 특유의 동적 키 및 배열 구조를 처리하는 파서 구현체.

@Component
public class GgApiParser implements ApiParser {

    @Override
    public boolean supports(String urlType) {
        return "GG".equals(urlType);
    }

    @Override
    public ExtractedDataResult parse(Map<String, Object> rawResponse, String apiKey) {
        // objectMapper로 자동 매핑 불가. 원본 통째로 주입.
        GgApiResponse response = new GgApiResponse(
                (Map<String, List<Map<String, Object>>>) (Object) rawResponse);

        // DTO 내부의 내장 비즈니스 메서드를 통해 데이터를 분리.
        int totalCount = response.extractTotalCount(apiKey);
        List<Map<String, Object>> records = response.extractRows(apiKey);

        return new ExtractedDataResult(totalCount, records);
    }
}
