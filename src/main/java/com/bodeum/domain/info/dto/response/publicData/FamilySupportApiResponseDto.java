package com.bodeum.domain.info.dto.response.publicData;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record FamilySupportApiResponseDto(
        @JsonProperty("page") Integer page,
        @JsonProperty("perPage") Integer perPage,
        @JsonProperty("totalCount") Integer totalCount,
        @JsonProperty("currentCount") Integer currentCount,
        @JsonProperty("matchCount") Integer matchCount,
        @JsonProperty("data") List<HeaderData> data
) {
    public record HeaderData(
            @JsonProperty("제공기관_명") String name,
            @JsonProperty("주소") String address,
            @JsonProperty("주소_상세") String addressDetail,
            @JsonProperty("전화번호") String phone,
            @JsonProperty("시도") String sido,
            @JsonProperty("시군구") String sigungu,
            @JsonProperty("대표자명") String representative,
            @JsonProperty("사업명") String businessName,
            @JsonProperty("사업유형") String businessType,
            @JsonProperty("우편번호") Integer zipCode
    ) {
        public String getFullAddress() {
            if (address == null || address.isBlank()) {
                return "";
            }
            if (addressDetail != null && !addressDetail.isBlank()) {
                return address.trim() + " " + addressDetail.trim();
            }
            return address.trim();
        }

        public String toExternalId() {
            String cleanName = (name != null) ? name.trim() : "";
            String cleanAddr = (address != null) ? address.trim() : "";
            return "FAMILY_SUPPORT_" + cleanName + "_" + cleanAddr;
        }
    }
}