package com.bodeum.domain.info.dto.response.publicData;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement(localName = "wantedList")
public record NationalWelfareApiResponseDto(
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
            @JacksonXmlProperty(localName = "jurMnofNm") String jurMnofNm,
            @JacksonXmlProperty(localName = "jurOrgNm") String jurOrgNm,
            @JacksonXmlProperty(localName = "intrsThemaArray") String intrsThemaArray,
            @JacksonXmlProperty(localName = "lifeArray") String lifeArray,
            @JacksonXmlProperty(localName = "trgterIndvdlArray") String trgterIndvdlArray,
            @JacksonXmlProperty(localName = "rprsCtadr") String rprsCtadr,
            @JacksonXmlProperty(localName = "servDtlLink") String servDtlLink
    ) {
        public String toExternalId() {
            return servId;
        }
    }
}