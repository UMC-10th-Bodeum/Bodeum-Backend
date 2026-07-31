package com.bodeum.domain.info.dto.response.publicData;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record LifelongEduCenterApiResponseDto(
        @JsonProperty("currentCount") Integer currentCount,
        @JsonProperty("data") List<HeaderData> data,
        @JsonProperty("matchCount") Integer matchCount,
        @JsonProperty("page") Integer page,
        @JsonProperty("perPage") Integer perPage,
        @JsonProperty("totalCount") Integer totalCount
) {
    public record HeaderData(
            @JsonProperty("기관명") String institutionName,
            @JsonProperty("기관종류") String institutionType,
            @JsonProperty("상세주소") String address,
            @JsonProperty("지역") String regionName,
            @JsonProperty("우편번호") Object zipCode,
            @JsonProperty("대표전화번호") String phone,
            @JsonProperty("팩스번호") String fax,
            @JsonProperty("누리집") String homepageUrl,
            @JsonProperty("기준일자") String baseDate
    ) {
        public String toExternalId() {
            return "LIFELONG_EDU_" + (institutionName != null ? institutionName.trim() : "UNKNOWN");
        }

        public String getFormattedHomepageUrl() {
            if (homepageUrl == null || homepageUrl.isBlank() || "정보없음".equalsIgnoreCase(homepageUrl.trim())) {
                return null;
            }
            String url = homepageUrl.trim();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return "https://" + url;
            }
            return url;
        }

        public String getFormattedFax() {
            if (fax == null || fax.isBlank() || "정보없음".equalsIgnoreCase(fax.trim())) {
                return null;
            }
            return fax.trim();
        }
    }
}