package com.bodeum.domain.info.dto.response.publicData;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PublicDoctorApiResponseDto(
        @JsonProperty("currentCount") int currentCount,
        @JsonProperty("matchCount") int matchCount,
        @JsonProperty("page") int page,
        @JsonProperty("perPage") int perPage,
        @JsonProperty("totalCount") int totalCount,
        @JsonProperty("data") List<HeaderData> data
) {
    public record HeaderData(
            @JsonProperty("구분") String category,
            @JsonProperty("서비스유형") String serviceType,
            @JsonProperty("요양기관명") String name,
            @JsonProperty("주소") String address
    ) {
        // ExternalId 생성 (식별태그 + 요양기관명 + 주소)
        public String toExternalId() {
            return "PUBLIC_DOCTOR_" + name + "_" + address;
        }
    }
}