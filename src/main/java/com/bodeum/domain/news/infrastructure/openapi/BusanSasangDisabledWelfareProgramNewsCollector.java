package com.bodeum.domain.news.infrastructure.openapi;

import com.bodeum.global.infrastructure.openapi.OdcloudClient;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "bodeum.news.public-data.sources.busan-sasang",
        name = "enabled",
        havingValue = "true"
)
public class BusanSasangDisabledWelfareProgramNewsCollector
        extends AbstractDisabledWelfareProgramNewsCollector {

    public static final String SOURCE_NAME = "부산광역시 사상구 장애인복지관 프로그램 현황";
    public static final String API_RESOURCE_PATH =
            "/15025718/v1/uddi:8dbb7a4c-46a1-43d1-b0f2-bcd49d756104";
    public static final String DATA_PORTAL_URL =
            "https://www.data.go.kr/data/15025718/fileData.do#tab-layer-openapi";
    public static final String DATA_DATE = "2026-07-20";

    public BusanSasangDisabledWelfareProgramNewsCollector(OdcloudClient odcloudClient) {
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
        return "busan-sasang";
    }

    @Override
    protected String regionLevel1() {
        return "부산광역시";
    }

    @Override
    protected DisabledWelfareProgramData toData(Map<String, Object> row) {
        return DisabledWelfareProgramData.fromSasangOdcloud(row, DATA_DATE);
    }
}
