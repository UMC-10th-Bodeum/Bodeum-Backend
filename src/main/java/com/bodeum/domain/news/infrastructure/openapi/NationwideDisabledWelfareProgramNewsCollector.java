package com.bodeum.domain.news.infrastructure.openapi;

import com.bodeum.domain.news.collector.NewsCandidate;
import com.bodeum.global.infrastructure.openapi.DataGoOpenApiClient;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class NationwideDisabledWelfareProgramNewsCollector extends AbstractDataGoNewsCollector {

    public static final String SOURCE_NAME = "전국 장애인복지관 운영 프로그램 표준데이터";
    public static final String API_RESOURCE_PATH =
            "/tn_pubr_public_disabled_welfare_center_program_api";
    public static final String DATA_PORTAL_URL =
            "https://www.data.go.kr/data/15155700/standard.do";
    private static final Set<String> REGIONS_COVERED_BY_DEDICATED_COLLECTORS = Set.of(
            "경기도 가평군",
            "경기도 과천시",
            "경기도 구리시",
            "경기도 군포시",
            "경기도 김포시",
            "경기도 동두천시",
            "경기도 부천시",
            "경기도 성남시",
            "경기도 수원시",
            "경기도 시흥시",
            "경기도 안성시",
            "경기도 양주시",
            "경기도 양평군",
            "경기도 오산시",
            "경기도 용인시",
            "경기도 의왕시",
            "경기도 의정부시",
            "경기도 이천시",
            "경기도 파주시",
            "경기도 평택시",
            "경기도 하남시",
            "부산광역시 사상구"
    );

    public NationwideDisabledWelfareProgramNewsCollector(DataGoOpenApiClient dataGoOpenApiClient) {
        super(dataGoOpenApiClient);
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
    protected NewsCandidate map(Map<String, Object> row) {
        String regionLevel1 = normalizeRegionLevel1(text(row, "ctpvNm", "CTPV_NM"));
        DisabledWelfareProgramData data = DisabledWelfareProgramData.fromDataGoStandard(row);
        String regionName = fullRegionName(regionLevel1, data.sigungu());
        if (!StringUtils.hasText(regionName)
                || REGIONS_COVERED_BY_DEDICATED_COLLECTORS.contains(regionName)) {
            return null;
        }

        String providerCode = text(row, "insttCode", "instt_code");
        String identity = valueOrEmpty(providerCode) + "|" + data.detailedIdentity();
        return DisabledWelfareProgramNewsMapper.map(
                data,
                "nationwide-welfare-program",
                identity,
                regionLevel1,
                sourceListUrl()
        );
    }

    private String normalizeRegionLevel1(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "강원도" -> "강원특별자치도";
            case "전라북도" -> "전북특별자치도";
            case "제주도" -> "제주특별자치도";
            default -> value;
        };
    }

    private String fullRegionName(String regionLevel1, String regionLevel2) {
        if (!StringUtils.hasText(regionLevel1) || !StringUtils.hasText(regionLevel2)) {
            return null;
        }
        return regionLevel1 + " " + regionLevel2;
    }

    private String text(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = row.get(key);
            if (value != null) {
                String text = String.valueOf(value).trim();
                if (StringUtils.hasText(text) && !"null".equalsIgnoreCase(text)) {
                    return text;
                }
            }
        }
        return null;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
