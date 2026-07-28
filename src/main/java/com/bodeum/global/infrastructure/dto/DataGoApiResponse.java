package com.bodeum.global.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

// (apis.data.go.kr) API의 계층형 응답 매핑. response -> body -> items -> item 형태.

public record DataGoApiResponse(
        @JsonProperty("response") Response response // 최상위 'response'
) {
    // response 내부 구조 정의
    public record Response(
            @JsonProperty("header") Header header, // 결과 코드와 메시지가 담긴 헤더
            @JsonProperty("body") Body body        // 실제 데이터와 페이징 정보가 담긴 바디
    ) {}

    // header 내부 구조 정의
    public record Header(
            @JsonProperty("resultCode") String resultCode, // 응답 결과 코드 (ex: 00)
            @JsonProperty("resultMsg") String resultMsg    // 응답 결과 메시지 (ex: NORMAL SERVICE)
    ) {}

    // body 내부 구조 정의
    public record Body(
            @JsonProperty("items") Items items,       // 데이터 리스트를 한 번 더 감싸고 있는 상자
            @JsonProperty("numOfRows") int numOfRows,   // 한 페이지 결과 수
            @JsonProperty("pageNo") int pageNo,         // 페이지 번호
            @JsonProperty("totalCount") int totalCount  // 전체 데이터 개수
    ) {}

    // items 내부 구조 정의
    public record Items(
            @JsonProperty("item") List<Map<String, Object>> item // 실제 데이터 목록 배열
    ) {}
}