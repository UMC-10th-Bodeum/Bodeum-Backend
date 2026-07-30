package com.bodeum.domain.info.dto.response.publicData;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DisabilityJobApiResponseDto(
        @JsonProperty("currentCount") Integer currentCount,
        @JsonProperty("data") List<HeaderData> data,
        @JsonProperty("matchCount") Integer matchCount,
        @JsonProperty("page") Integer page,
        @JsonProperty("perPage") Integer perPage,
        @JsonProperty("totalCount") Integer totalCount
) {
    public record HeaderData(
            @JsonProperty("연번") Long serialNumber,
            @JsonProperty("사업장명") String workplaceName,
            @JsonProperty("사업장 주소") String address,
            @JsonProperty("모집직종") String recruitmentJob,
            @JsonProperty("고용형태") String employmentType,
            @JsonProperty("임금형태") String wageType,
            @JsonProperty("임금") Object wage, // 시급/월급 등 정수/실수 혼용 대비
            @JsonProperty("모집기간") String recruitmentPeriod,
            @JsonProperty("요구경력") String requiredExperience,
            @JsonProperty("요구학력") String requiredEducation,
            @JsonProperty("기업형태") String companyType,
            @JsonProperty("연락처") String contact,
            @JsonProperty("담당기관") String agency,
            @JsonProperty("등록일") String registrationDate,
            @JsonProperty("구인신청일자") String applicationDate
    ) {
        public String toExternalId() {
            return "JOB_" + (serialNumber != null ? serialNumber : workplaceName + "_" + registrationDate);
        }
    }
}