package com.bodeum.domain.news.infrastructure.openapi;

import com.bodeum.domain.news.collector.NewsCandidate;
import com.bodeum.global.infrastructure.openapi.OdcloudClient;
import java.util.Map;

public abstract class AbstractDisabledWelfareProgramNewsCollector extends AbstractOdcloudNewsCollector {

    protected AbstractDisabledWelfareProgramNewsCollector(OdcloudClient odcloudClient) {
        super(odcloudClient);
    }

    protected abstract String externalIdPrefix();

    protected String regionLevel1() {
        return "경기도";
    }

    protected DisabledWelfareProgramData toData(Map<String, Object> row) {
        return DisabledWelfareProgramData.fromOdcloud(row);
    }

    @Override
    protected final NewsCandidate map(Map<String, Object> row) {
        DisabledWelfareProgramData data = toData(row);
        return DisabledWelfareProgramNewsMapper.map(
                data,
                externalIdPrefix(),
                data.basicIdentity(),
                regionLevel1(),
                sourceListUrl()
        );
    }
}
