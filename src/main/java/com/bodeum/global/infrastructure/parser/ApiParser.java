package com.bodeum.global.infrastructure.parser;

import com.bodeum.global.infrastructure.dto.ExtractedDataResult;
import java.util.Map;

// 외부 API 응답을 시스템 규격으로 정형화하는 인터페이스.

public interface ApiParser {

    /**
     * 해당 파서가 지원하는 URL 유형인지 확인.
     * @param urlType 오픈 API 유형 (ODCLOUD, DATAGO, GG, ETC)
     * @return 지원 여부 (true/false)
     */
    boolean supports(String urlType);

    /**
     * 로우(Raw) 응답 데이터를 분석하여 공통 규격 객체로 변환.
     * @param rawResponse RestClient가 수집한 원본 Map 데이터
     * @param apiKey 경기도 등 특정 API에서 필요한 동적 키값 (필요 없으면 null)
     * @return 정형화된 총 개수와 레코드 목록
     */
    ExtractedDataResult parse(Map<String, Object> rawResponse, String apiKey);
}
