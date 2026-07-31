package com.bodeum.domain.info.dto.response.publicData;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement(localName = "wantedList")
public record LocalWelfareApiResponseDto(
        @JacksonXmlProperty(localName = "totalCount") Integer totalCount,
        @JacksonXmlProperty(localName = "pageNo") Integer pageNo,
        @JacksonXmlProperty(localName = "numOfRows") Integer numOfRows,
        @JacksonXmlProperty(localName = "resultCode") String resultCode,
        @JacksonXmlProperty(localName = "resultMessage") String resultMessage,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "servList") List<ServList> servList
) {
    public record ServList(
            @JacksonXmlProperty(localName = "servId") String servId,
            @JacksonXmlProperty(localName = "servNm") String servNm,
            @JacksonXmlProperty(localName = "servDgst") String servDgst,
            @JacksonXmlProperty(localName = "ctpvNm") String ctpvNm,          // 시도명 (ex: 경기도)
            @JacksonXmlProperty(localName = "sggNm") String sggNm,            // 시군구명 (ex: 성남시)
            @JacksonXmlProperty(localName = "bizChrDeptNm") String bizChrDeptNm, // 담당부서
            @JacksonXmlProperty(localName = "aplyMtdNm") String aplyMtdNm,     // 신청방법 (ex: 인터넷, 방문)
            @JacksonXmlProperty(localName = "intrsThemaNmArray") String intrsThemaNmArray,
            @JacksonXmlProperty(localName = "lifeNmArray") String lifeNmArray,
            @JacksonXmlProperty(localName = "trgterIndvdlNmArray") String trgterIndvdlNmArray,
            @JacksonXmlProperty(localName = "servDtlLink") String servDtlLink // 상세 URL
    ) {
        public String toExternalId() {
            return servId;
        }
    }
}