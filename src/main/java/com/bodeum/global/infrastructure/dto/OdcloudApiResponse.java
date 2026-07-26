package com.bodeum.global.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

// (api.odcloud.kr) API의 응답 형식 매핑.

public record OdcloudApiResponse(
        @JsonProperty("page") int page,                  // 현재 요청한 페이지 번호
        @JsonProperty("perPage") int perPage,              // 한 페이지당 보여줄 데이터 개수
        @JsonProperty("totalCount") int totalCount,        // 공공데이터포털에 등록된 전체 데이터 개수
        @JsonProperty("currentCount") int currentCount,    // 현재 페이지에 담긴 데이터 개수
        @JsonProperty("data") List<Map<String, Object>> data // 실제 기관 정보들이 Key-Value 형태로 담긴 배열
) {}
