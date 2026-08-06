package com.bodeum.domain.info.dto.response.publicData;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TherapyRehabApiResponseDto(
        @JsonProperty("currentCount") int currentCount,
        @JsonProperty("matchCount") int matchCount,
        @JsonProperty("page") int page,
        @JsonProperty("perPage") int perPage,
        @JsonProperty("totalCount") int totalCount,
        @JsonProperty("data") List<HeaderData> data
) {
    public record HeaderData(
            @JsonProperty("도시군구") String sigungu,
            @JsonProperty("제공 기관명") String name,
            @JsonProperty("주소") String address
    ) {}
}