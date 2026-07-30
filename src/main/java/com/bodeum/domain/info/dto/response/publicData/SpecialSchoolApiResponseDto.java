package com.bodeum.domain.info.dto.response.publicData;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SpecialSchoolApiResponseDto(
        @JsonProperty("currentCount") Integer currentCount,
        @JsonProperty("data") List<HeaderData> data,
        @JsonProperty("matchCount") Integer matchCount,
        @JsonProperty("page") Integer page,
        @JsonProperty("perPage") Integer perPage,
        @JsonProperty("totalCount") Integer totalCount
) {
    public record HeaderData(
            @JsonProperty("기관명") String schoolName,
            @JsonProperty("설립별") String foundationType, // 국립, 공립, 사립 등
            @JsonProperty("장애영역") String disabilityTarget, // 시각장애, 청각장애, 지적장애 등
            @JsonProperty("주소") String address,
            @JsonProperty("시도") String sidoName,
            @JsonProperty("우편번호") Object zipCode,
            @JsonProperty("교무실") String officePhone,
            @JsonProperty("행정실") String adminPhone,
            @JsonProperty("교장실") String principalPhone,
            @JsonProperty("팩스") String fax,
            @JsonProperty("누리집") String homepageUrl,
            @JsonProperty("교장명") String principalName,
            @JsonProperty("개교년월일") String openingDate,
            @JsonProperty("인가년월일") String approvalDate,
            @JsonProperty("기준일자") String baseDate
    ) {
        public String toExternalId() {
            return "SPECIAL_SCH_" + (schoolName != null ? schoolName.trim() : "UNKNOWN");
        }

        public String getPrimaryPhone() {
            if (officePhone != null && !officePhone.isBlank()) {
                return officePhone.trim();
            }
            if (adminPhone != null && !adminPhone.isBlank()) {
                return adminPhone.trim();
            }
            return null;
        }

        public String getFormattedHomepageUrl() {
            if (homepageUrl == null || homepageUrl.isBlank()) {
                return null;
            }
            String url = homepageUrl.trim();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return "https://" + url;
            }
            return url;
        }
    }
}