package com.bodeum.domain.news.infrastructure.openapi;

import com.bodeum.domain.news.collector.NewsCandidate;
import com.bodeum.global.infrastructure.openapi.GgOpenApiClient;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "bodeum.news.public-data.sources.gyeonggi",
        name = "enabled",
        havingValue = "true"
)
public class GyeonggiDisabledWelfareProgramNewsCollector extends AbstractGgNewsCollector {

    public static final String SOURCE_NAME = "경기도 장애인복지관 운영 프로그램";
    public static final String API_RESOURCE_PATH = "/DspsnCmwelfctOpertProg";
    public static final String API_RESPONSE_KEY = "DspsnCmwelfctOpertProg";
    public static final String DATA_PORTAL_URL =
            "https://data.gg.go.kr/portal/data/service/selectServicePage.do"
                    + "?infId=I5KFBR9J388NS75BEGWD29433604&infSeq=2";

    private static final Set<String> INDIVIDUALLY_COLLECTED_REGIONS = Set.of(
            "구리시",
            "성남시",
            "수원시"
    );

    public GyeonggiDisabledWelfareProgramNewsCollector(GgOpenApiClient ggOpenApiClient) {
        super(ggOpenApiClient);
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
    protected String responseKey() {
        return API_RESPONSE_KEY;
    }

    @Override
    protected NewsCandidate map(Map<String, Object> row) {
        DisabledWelfareProgramData data = DisabledWelfareProgramData.fromGgOpenApi(row);
        if (INDIVIDUALLY_COLLECTED_REGIONS.contains(data.sigungu())) {
            return null;
        }

        return DisabledWelfareProgramNewsMapper.map(
                data,
                "gg-welfare-program",
                data.detailedIdentity(),
                "경기도",
                sourceListUrl()
        );
    }
}
