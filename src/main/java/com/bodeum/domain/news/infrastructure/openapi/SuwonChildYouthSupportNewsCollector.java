package com.bodeum.domain.news.infrastructure.openapi;

import com.bodeum.domain.news.collector.NewsCandidate;
import com.bodeum.domain.news.entity.NewsType;
import com.bodeum.global.infrastructure.openapi.OdcloudClient;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SuwonChildYouthSupportNewsCollector extends AbstractOdcloudNewsCollector {

    public static final String SOURCE_NAME = "경기도 수원시 장애아동청소년지원서비스현황";
    public static final String CATEGORY_NAME = "SUPPORT_SERVICE";
    public static final String API_RESOURCE_PATH =
            "/15040644/v1/uddi:346a8e04-45d2-4dad-9fae-f0558a5ab1b1";
    public static final String DATA_PORTAL_URL =
            "https://www.data.go.kr/data/15040644/fileData.do#tab-layer-openapi";

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    public SuwonChildYouthSupportNewsCollector(OdcloudClient odcloudClient) {
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
    protected NewsCandidate map(Map<String, Object> row) {
        String institution = trim(row.get("지정기관명"));
        if (!StringUtils.hasText(institution)) {
            return null;
        }

        String service = trim(row.get("제공서비스"));
        String target = trim(row.get("서비스대상"));
        String roadAddress = trim(row.get("도로명주소"));
        String lotAddress = trim(row.get("지번주소"));
        String contact = trim(row.get("연락처"));
        String sido = trim(row.get("시도명"));
        String sigungu = trim(row.get("시군명"));
        LocalDate designationStart = parseDate(row.get("지정시작일"));
        LocalDate designationEnd = parseDate(row.get("지정종료일"));
        LocalDate dataDate = parseDate(row.get("데이터기준일자"));
        LocalDateTime publishedAt = (dataDate == null ? LocalDate.now(SERVICE_ZONE) : dataDate)
                .atStartOfDay();

        String title = limit(
                institution
                        + (StringUtils.hasText(service) ? " " + service + " 지원 서비스" : " 지원 서비스"),
                150
        );
        String summary = limit(buildSummary(service, target), 500);
        String content = buildContent(
                institution,
                service,
                target,
                roadAddress,
                lotAddress,
                contact,
                designationStart,
                designationEnd
        );
        String regionName = join(sido, sigungu);
        String externalItemId = externalId(institution, roadAddress, lotAddress, service);

        return new NewsCandidate(
                externalItemId,
                title,
                summary,
                content,
                institution,
                DATA_PORTAL_URL,
                null,
                regionName,
                target,
                contact,
                null,
                publishedAt,
                null,
                null,
                null,
                null,
                CATEGORY_NAME,
                NewsType.LOCAL,
                null
        );
    }

    private String buildSummary(String service, String target) {
        List<String> values = new ArrayList<>();
        if (StringUtils.hasText(service)) {
            values.add("제공 서비스: " + service);
        }
        if (StringUtils.hasText(target)) {
            values.add("서비스 대상: " + target);
        }
        return values.isEmpty() ? null : String.join(" | ", values);
    }

    private String buildContent(
            String institution,
            String service,
            String target,
            String roadAddress,
            String lotAddress,
            String contact,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<String> lines = new ArrayList<>();
        lines.add("기관명: " + institution);
        addLine(lines, "제공 서비스", service);
        addLine(lines, "서비스 대상", target);
        addLine(lines, "도로명 주소", roadAddress);
        addLine(lines, "지번 주소", lotAddress);
        addLine(lines, "연락처", contact);
        addLine(lines, "지정 시작일", startDate == null ? null : startDate.toString());
        addLine(lines, "지정 종료일", endDate == null ? null : endDate.toString());
        return String.join("\n", lines);
    }

    private void addLine(List<String> lines, String label, String value) {
        if (StringUtils.hasText(value)) {
            lines.add(label + ": " + value);
        }
    }

    private String externalId(String institution, String roadAddress, String lotAddress, String service) {
        String seed = String.join(
                "|",
                institution,
                valueOrEmpty(roadAddress),
                valueOrEmpty(lotAddress),
                valueOrEmpty(service)
        );
        return "suwon-" + UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    private LocalDate parseDate(Object value) {
        String text = trim(value);
        return StringUtils.hasText(text) ? LocalDate.parse(text) : null;
    }

    private String trim(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return StringUtils.hasText(text) ? text : null;
    }

    private String join(String first, String second) {
        if (!StringUtils.hasText(first)) {
            return second;
        }
        if (!StringUtils.hasText(second)) {
            return first;
        }
        return first + " " + second;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String limit(String value, int maxLength) {
        return value != null && value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
