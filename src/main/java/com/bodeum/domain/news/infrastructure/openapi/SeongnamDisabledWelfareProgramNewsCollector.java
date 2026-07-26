package com.bodeum.domain.news.infrastructure.openapi;

import com.bodeum.global.infrastructure.openapi.OdcloudClient;
import org.springframework.stereotype.Component;

@Component
public class SeongnamDisabledWelfareProgramNewsCollector
        extends AbstractDisabledWelfareProgramNewsCollector {

    public static final String SOURCE_NAME = "경기도 성남시 장애인복지관 운영 프로그램";
    public static final String API_RESOURCE_PATH =
            "/15037403/v1/uddi:583e49f9-47b5-4c64-b0b0-86b12f59efba";
    public static final String DATA_PORTAL_URL =
            "https://www.data.go.kr/data/15037403/fileData.do#tab-layer-openapi";

    public SeongnamDisabledWelfareProgramNewsCollector(OdcloudClient odcloudClient) {
        super(odcloudClient);
    }

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    @Override
    public String sourceListUrl() {
        return DATA_PORTAL_URL;
    }

    @Override
    protected String resourcePath() {
        return API_RESOURCE_PATH;
    }

    @Override
    protected String externalIdPrefix() {
        return "seongnam";
    }
}
