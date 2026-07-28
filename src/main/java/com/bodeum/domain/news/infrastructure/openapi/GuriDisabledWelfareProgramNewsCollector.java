package com.bodeum.domain.news.infrastructure.openapi;

import com.bodeum.global.infrastructure.openapi.OdcloudClient;
import org.springframework.stereotype.Component;

@Component
public class GuriDisabledWelfareProgramNewsCollector
        extends AbstractDisabledWelfareProgramNewsCollector {

    public static final String SOURCE_NAME = "경기도 구리시 장애인복지관 운영 프로그램";
    public static final String API_RESOURCE_PATH =
            "/15108078/v1/uddi:d5b669c5-6ade-43db-98e7-a592dbf6835c";
    public static final String DATA_PORTAL_URL =
            "https://www.data.go.kr/data/15108078/fileData.do#tab-layer-openapi";

    public GuriDisabledWelfareProgramNewsCollector(OdcloudClient odcloudClient) {
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
        return "guri";
    }
}
