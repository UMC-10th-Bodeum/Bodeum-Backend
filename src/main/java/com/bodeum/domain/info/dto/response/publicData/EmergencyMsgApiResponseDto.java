package com.bodeum.domain.info.dto.response.publicData;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record EmergencyMsgApiResponseDto(
        @JsonProperty("response") ResponseData response
) {
    public record ResponseData(
            @JsonProperty("header") Header header,
            @JsonProperty("body") Body body
    ) {}

    public record Header(
            @JsonProperty("resultCode") String resultCode,
            @JsonProperty("resultMsg") String resultMsg
    ) {}

    public record Body(
            @JsonProperty("items") Items items,
            @JsonProperty("numOfRows") Integer numOfRows,
            @JsonProperty("pageNo") Integer pageNo,
            @JsonProperty("totalCount") Integer totalCount
    ) {}

    public record Items(
            @JsonProperty("item") List<Item> item
    ) {}

    public record Item(
            @JsonProperty("dutyAddr") String dutyAddr,
            @JsonProperty("dutyName") String dutyName,
            @JsonProperty("emcOrgCod") String emcOrgCod,
            @JsonProperty("hpid") String hpid,
            @JsonProperty("rnum") Integer rnum,
            @JsonProperty("symBlkEndDtm") Long symBlkEndDtm,
            @JsonProperty("symBlkMsg") String symBlkMsg,
            @JsonProperty("symBlkMsgTyp") String symBlkMsgTyp,
            @JsonProperty("symBlkSttDtm") Long symBlkSttDtm,
            @JsonProperty("symOutDspMth") String symOutDspMth,
            @JsonProperty("symOutDspYon") String symOutDspYon,
            @JsonProperty("symTypCod") String symTypCod,
            @JsonProperty("symTypCodMag") String symTypCodMag,
            @JsonProperty("trtPrtCodMag") String trtPrtCodMag
    ) {
        public String toExternalId() {
            return hpid + "_" + rnum;
        }
    }
}