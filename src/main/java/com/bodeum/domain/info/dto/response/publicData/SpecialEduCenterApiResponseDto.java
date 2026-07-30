package com.bodeum.domain.info.dto.response.publicData;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SpecialEduCenterApiResponseDto(
        @JsonProperty("currentCount") Integer currentCount,
        @JsonProperty("data") List<HeaderData> data,
        @JsonProperty("matchCount") Integer matchCount,
        @JsonProperty("page") Integer page,
        @JsonProperty("perPage") Integer perPage,
        @JsonProperty("totalCount") Integer totalCount
) {
    public record HeaderData(
            @JsonProperty("센터명") String centerName,
            @JsonProperty("교육청") String officeOfEducation,
            @JsonProperty("설치된기관") String installedInstitution,
            @JsonProperty("기관주소") String address,
            @JsonProperty("시도") String sidoName,
            @JsonProperty("우편번호") Object zipCode,
            @JsonProperty("전화번호") String phone,
            @JsonProperty("팩스번호") String fax,
            @JsonProperty("누리집주소") String homepageUrl,
            @JsonProperty("기준일자") String baseDate
    ) {
        public String toExternalId() {
            return "SPECIAL_EDU_" + (centerName != null ? centerName.trim() : officeOfEducation);
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