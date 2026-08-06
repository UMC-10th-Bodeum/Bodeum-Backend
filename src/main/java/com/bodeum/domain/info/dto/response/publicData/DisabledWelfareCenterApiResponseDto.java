package com.bodeum.domain.info.dto.response.publicData;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DisabledWelfareCenterApiResponseDto(
        @JsonProperty("currentCount") int currentCount,
        @JsonProperty("matchCount") int matchCount,
        @JsonProperty("page") int page,
        @JsonProperty("perPage") int perPage,
        @JsonProperty("totalCount") int totalCount,
        @JsonProperty("data") List<HeaderData> data
) {
    public record HeaderData(
            @JsonProperty("연번") Long id,
            @JsonProperty("시설명") String name,
            @JsonProperty("시설 주소") String address,
            @JsonProperty("시도") String sido,
            @JsonProperty("시군구") String sigungu,
            @JsonProperty("전화번호") String phone,
            @JsonProperty("팩스번호") String fax,
            @JsonProperty("시설유형") String facilityType,
            @JsonProperty("법인현황") String corporateStatus,
            @JsonProperty("종사자정원") Integer employeeQuota,
            @JsonProperty("종사자 현원") Integer employeeCount
    ) {
        // ExternalId 생성 편의 메서드 (식별태그 + 시설명 + 주소)
        public String toExternalId() {
            return "DISABLED_WELFARE_CENTER_" + name + "_" + address;
        }
    }
}