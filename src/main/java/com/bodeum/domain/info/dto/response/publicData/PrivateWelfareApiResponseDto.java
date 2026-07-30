package com.bodeum.domain.info.dto.response.publicData;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PrivateWelfareApiResponseDto(
        @JsonProperty("currentCount") Integer currentCount,
        @JsonProperty("data") List<HeaderData> data,
        @JsonProperty("matchCount") Integer matchCount,
        @JsonProperty("page") Integer page,
        @JsonProperty("perPage") Integer perPage,
        @JsonProperty("totalCount") Integer totalCount
) {
    public record HeaderData(
            @JsonProperty("가구상황") String householdSituation,
            @JsonProperty("관심주제") String topic,
            @JsonProperty("기관명") String organizationName,
            @JsonProperty("기타") String etc,
            @JsonProperty("사업명") String businessName,
            @JsonProperty("사업목적") String businessPurpose,
            @JsonProperty("사업시작일") String startDate,
            @JsonProperty("사업종료일") String endDate,
            @JsonProperty("생애주기") String lifeCycle,
            @JsonProperty("신청방법") String applicationMethod,
            @JsonProperty("제출서류") String requiredDocuments,
            @JsonProperty("지원내용") String supportContent,
            @JsonProperty("지원대상") String supportTarget
    ) {
        public String toExternalId() {
            String org = organizationName != null ? organizationName.trim() : "UNKNOWN";
            String biz = businessName != null ? businessName.trim() : "UNKNOWN";
            return org + "_" + biz;
        }
    }
}