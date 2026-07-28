package com.bodeum.domain.news.infrastructure.openapi;

import com.bodeum.domain.news.collector.NewsCandidate;
import com.bodeum.domain.news.entity.NewsType;
import com.bodeum.global.infrastructure.openapi.OdcloudClient;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.util.StringUtils;

public abstract class AbstractDisabledWelfareProgramNewsCollector extends AbstractOdcloudNewsCollector {

    private static final String DEFAULT_CATEGORY_NAME = "WELFARE_PROGRAM";
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    protected AbstractDisabledWelfareProgramNewsCollector(OdcloudClient odcloudClient) {
        super(odcloudClient);
    }

    protected abstract String externalIdPrefix();

    protected String provinceName() {
        return "경기도";
    }

    @Override
    protected final NewsCandidate map(Map<String, Object> row) {
        String programName = text(row.get("프로그램명"));
        if (!StringUtils.hasText(programName)) {
            return null;
        }

        String facility = text(row.get("복지관명"));
        String category = text(row.get("구분"));
        String detailCategory = text(row.get("상세구분"));
        String programContent = text(row.get("프로그램내용"));
        String target = text(row.get("이용대상"));
        String disabilityCondition = text(row.get("이용대상상세조건(장애유형)"));
        String ageCondition = text(row.get("이용대상상세조건(연령제한)"));
        String otherCondition = text(row.get("이용대상상세조건(기타조건)"));
        String usageTime = text(row.get("이용시간"));
        String fee = formatMoney(firstValue(row, "이용금액", "이용금액(원)"), "무료");
        String feeBasis = text(row.get("이용금액산정기준"));
        String additionalCost = formatMoney(row.get("부가비용"), "없음");
        String roadAddress = text(row.get("소재지도로명주소"));
        String lotAddress = text(row.get("소재지지번주소"));
        String contact = firstText(row, "대표전화번호", "전화번호");
        String postalCode = text(row.get("우편번호"));
        String latitude = text(row.get("위도"));
        String longitude = text(row.get("경도"));
        String sigungu = text(row.get("시군명"));
        LocalDate dataDate = parseDate(firstValue(row, "데이터기준일자", "데이터기준일"));
        LocalDateTime publishedAt = (dataDate == null ? LocalDate.now(SERVICE_ZONE) : dataDate)
                .atStartOfDay();

        String targetAudience = limit(buildTargetAudience(
                target,
                disabilityCondition,
                ageCondition,
                otherCondition
        ), 500);
        String content = buildContent(
                programContent,
                facility,
                category,
                detailCategory,
                target,
                disabilityCondition,
                ageCondition,
                otherCondition,
                usageTime,
                fee,
                feeBasis,
                additionalCost,
                roadAddress,
                lotAddress,
                contact,
                postalCode,
                latitude,
                longitude,
                dataDate
        );

        return new NewsCandidate(
                externalId(facility, programName),
                limit(programName, 150),
                limit(programContent, 500),
                content,
                facility,
                sourceListUrl(),
                null,
                join(provinceName(), sigungu),
                targetAudience,
                contact,
                null,
                publishedAt,
                null,
                null,
                null,
                null,
                StringUtils.hasText(category) ? limit(category, 50) : DEFAULT_CATEGORY_NAME,
                NewsType.ACTIVITY,
                null
        );
    }

    private String buildTargetAudience(
            String target,
            String disabilityCondition,
            String ageCondition,
            String otherCondition
    ) {
        List<String> values = new ArrayList<>();
        addLine(values, "이용 대상", target);
        addLine(values, "장애 유형", disabilityCondition);
        addLine(values, "연령 제한", ageCondition);
        addLine(values, "기타 조건", otherCondition);
        return values.isEmpty() ? null : String.join(" | ", values);
    }

    private String buildContent(
            String programContent,
            String facility,
            String category,
            String detailCategory,
            String target,
            String disabilityCondition,
            String ageCondition,
            String otherCondition,
            String usageTime,
            String fee,
            String feeBasis,
            String additionalCost,
            String roadAddress,
            String lotAddress,
            String contact,
            String postalCode,
            String latitude,
            String longitude,
            LocalDate dataDate
    ) {
        List<String> lines = new ArrayList<>();
        addLine(lines, "프로그램 내용", programContent);
        addLine(lines, "운영 기관", facility);
        addLine(lines, "구분", category);
        addLine(lines, "상세 구분", detailCategory);
        addLine(lines, "이용 대상", target);
        addLine(lines, "장애 유형 조건", disabilityCondition);
        addLine(lines, "연령 조건", ageCondition);
        addLine(lines, "기타 조건", otherCondition);
        addLine(lines, "이용 시간", usageTime);
        addLine(lines, "이용 금액", fee);
        addLine(lines, "이용 금액 산정 기준", feeBasis);
        addLine(lines, "부가 비용", additionalCost);
        addLine(lines, "도로명 주소", roadAddress);
        addLine(lines, "지번 주소", lotAddress);
        addLine(lines, "대표 전화번호", contact);
        addLine(lines, "우편번호", postalCode);
        addLine(lines, "위도", latitude);
        addLine(lines, "경도", longitude);
        addLine(lines, "데이터 기준일자", dataDate == null ? null : dataDate.toString());
        return String.join("\n", lines);
    }

    private String formatMoney(Object value, String zeroText) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            long amount = number.longValue();
            return amount == 0
                    ? zeroText
                    : NumberFormat.getIntegerInstance(Locale.KOREA).format(amount) + "원";
        }

        String text = text(value);
        return "0".equals(text) ? zeroText : text;
    }

    private Object firstValue(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = row.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstText(Map<String, Object> row, String... keys) {
        return text(firstValue(row, keys));
    }

    private String externalId(String facility, String programName) {
        String seed = valueOrEmpty(facility) + "|" + programName;
        return externalIdPrefix()
                + "-"
                + UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    private LocalDate parseDate(Object value) {
        String text = text(value);
        return StringUtils.hasText(text) ? LocalDate.parse(text) : null;
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return StringUtils.hasText(text) ? text : null;
    }

    private void addLine(List<String> lines, String label, String value) {
        if (StringUtils.hasText(value)) {
            lines.add(label + ": " + value);
        }
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
