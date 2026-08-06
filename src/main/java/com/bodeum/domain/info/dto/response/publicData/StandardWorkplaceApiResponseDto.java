package com.bodeum.domain.info.dto.response.publicData;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record StandardWorkplaceApiResponseDto(
        @JsonProperty("currentCount") Integer currentCount,
        @JsonProperty("data") List<HeaderData> data,
        @JsonProperty("matchCount") Integer matchCount,
        @JsonProperty("page") Integer page,
        @JsonProperty("perPage") Integer perPage,
        @JsonProperty("totalCount") Integer totalCount
) {
    public record HeaderData(
            @JsonProperty("관할지사") String agency,
            @JsonProperty("구분") String categoryType, // 일반, 자회사 등
            @JsonProperty("대표자") String ceoName,
            @JsonProperty("사업자등록번호") String businessRegistrationNumber,
            @JsonProperty("사업체명") String companyName,
            @JsonProperty("소재지") String address,
            @JsonProperty("업종 및 주요생산품") String businessTypeAndProducts,
            @JsonProperty("인증번호") String certNumber,
            @JsonProperty("인증일자") String certDate,
            @JsonProperty("전화번호") String phone
    ) {
        public String toExternalId() {
            if (certNumber != null && !certNumber.isBlank()) {
                return "STANDARD_WORK_" + certNumber.trim();
            }
            if (businessRegistrationNumber != null && !businessRegistrationNumber.isBlank()) {
                return "STANDARD_WORK_" + businessRegistrationNumber.trim();
            }
            return "STANDARD_WORK_" + companyName;
        }
    }
}