package com.bodeum.global.infrastructure.parser;

import com.bodeum.global.infrastructure.dto.ExtractedDataResult;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

// 제3의 API를 위한 파서 구현체.

@Component
public class EtcApiParser implements ApiParser {

    @Override
    public boolean supports(String urlType) {
        return "ETC".equals(urlType);
    }

    @Override
    public ExtractedDataResult parse(Map<String, Object> rawResponse, String apiKey) {
        // 구조를 알 수 없으므로, 현재 들어온 단건 혹은 통째 데이터를 하나의 레코드로 간주하여 리스트 주입.
        List<Map<String, Object>> records = List.of(rawResponse);
        return new ExtractedDataResult(1, records);
    }
}
