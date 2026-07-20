package com.bodeum.global.infrastructure.parser;

import com.bodeum.global.infrastructure.dto.ExtractedDataResult;
import com.bodeum.global.infrastructure.dto.OdcloudApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

// 오디클라우드 응답 포맷을 처리하는 파서 구현체.

@Component
public class OdcloudApiParser implements ApiParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(String urlType) {
        return "ODCLOUD".equals(urlType);
    }

    @Override
    public ExtractedDataResult parse(Map<String, Object> rawResponse, String apiKey) {
        // 원본 Map 데이터를 정의한 불변 레코드 객체로 변환.
        OdcloudApiResponse response = objectMapper.convertValue(rawResponse, OdcloudApiResponse.class);

        int totalCount = response.totalCount();
        List<Map<String, Object>> records = response.data();

        if (records == null) {
            records = List.of(); // NullPointerException 방지를 위한 빈 리스트 처리
        }

        return new ExtractedDataResult(totalCount, records);
    }
}