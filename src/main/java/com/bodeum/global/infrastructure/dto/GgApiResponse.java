package com.bodeum.global.infrastructure.dto;

import java.util.List;
import java.util.Map;

// (openapi.gg.go.kr)의 구조 파싱 및 제어.

public record GgApiResponse(
        // 이 API는 본인들의 API 주소명을 최상위 키로 던져서 Map으로 이를 받아 통째로 보관.
        Map<String, List<Map<String, Object>>> rawResponse
) {
    // 동적 API 키값 내부에서 실제 데이터 행(row) 배열만 안전하게 추출
    public List<Map<String, Object>> extractRows(String apiName) {
        if (rawResponse == null || !rawResponse.containsKey(apiName)) {
            return List.of(); // 찾는 API 키가 없으면 안전하게 빈 리스트 반환
        }
        List<Map<String, Object>> contents = rawResponse.get(apiName);
        if (contents == null || contents.size() < 2) {
            return List.of(); // head와 row 구조가 정상적으로 갖춰지지 않았다면 빈 리스트 반환
        }

        // API 구조상 0번째는 head 상자, 1번째가 row 상자.
        Map<String, Object> rowContainer = contents.get(1);
        if (rowContainer != null && rowContainer.containsKey("row")) {
            return (List<Map<String, Object>>) rowContainer.get("row"); // row 내부의 데이터 배열 반환
        }
        return List.of();
    }

    // 동적 API 키값 내부의 head에서 전체 데이터 개수(list_total_count) 추출.
    public int extractTotalCount(String apiName) {
        if (rawResponse == null || !rawResponse.containsKey(apiName)) {
            return 0;
        }
        try {
            List<Map<String, Object>> contents = rawResponse.get(apiName);
            // 0번째 요소인 head 상자 꺼냄.
            Map<String, Object> headContainer = contents.get(0);
            List<Map<String, Object>> headList = (List<Map<String, Object>>) headContainer.get("head");

            // headList의 0번째는 {"list_total_count": 총개수} 형태.
            Map<String, Object> totalCountMap = headList.get(0);
            return Integer.parseInt(String.valueOf(totalCountMap.get("list_total_count")));
        } catch (Exception e) {
            return 0; // 예외 발생 시 시스템 다운 방지를 위해 0 반환
        }
    }
}
