package com.bodeum.domain.news.infrastructure.openapi;

import com.bodeum.domain.news.collector.NewsCandidate;
import com.bodeum.domain.news.entity.NewsCategoryCode;
import com.bodeum.domain.news.entity.NewsType;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.util.StringUtils;

final class DisabledWelfareProgramNewsMapper {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private DisabledWelfareProgramNewsMapper() {
    }

    static NewsCandidate map(
            DisabledWelfareProgramData data,
            String externalIdPrefix,
            String externalIdentity,
            String regionLevel1,
            String sourceListUrl
    ) {
        return map(
                data,
                externalIdPrefix,
                externalIdentity,
                regionLevel1,
                data.sigungu(),
                sourceListUrl
        );
    }

    static NewsCandidate map(
            DisabledWelfareProgramData data,
            String externalIdPrefix,
            String externalIdentity,
            String regionLevel1,
            String regionLevel2,
            String sourceListUrl
    ) {
        if (!StringUtils.hasText(data.programName())) {
            return null;
        }

        LocalDate dataDate = parseDate(data.dataDate());
        LocalDateTime publishedAt = (dataDate == null ? LocalDate.now(SERVICE_ZONE) : dataDate)
                .atStartOfDay();
        String targetAudience = limit(buildTargetAudience(data), 500);
        NewsCategoryCode categoryCode = NewsCategoryClassifier.classifyActivity(data);

        return new NewsCandidate(
                externalId(externalIdPrefix, externalIdentity),
                limit(data.programName(), 150),
                limit(firstText(data.programContent(), data.usageDetail()), 500),
                buildContent(data, dataDate),
                limit(firstText(data.facility(), data.managingAgency()), 100),
                sourceListUrl,
                null,
                join(regionLevel1, regionLevel2),
                targetAudience,
                limit(firstText(
                        data.contact(),
                        data.additionalContact(),
                        data.managingAgencyContact()
                ), 100),
                limit(data.managingAgency(), 100),
                publishedAt,
                null,
                null,
                null,
                null,
                categoryCode,
                NewsType.ACTIVITY,
                null
        );
    }

    private static String buildTargetAudience(DisabledWelfareProgramData data) {
        List<String> values = new ArrayList<>();
        addLine(values, "이용 대상", data.target());
        addLine(values, "장애 유형", data.disabilityCondition());
        addLine(values, "연령 제한", data.ageCondition());
        addLine(values, "기타 조건", data.otherCondition());
        return values.isEmpty() ? null : String.join(" | ", values);
    }

    private static String buildContent(DisabledWelfareProgramData data, LocalDate dataDate) {
        List<String> lines = new ArrayList<>();
        addLine(lines, "프로그램 내용", data.programContent());
        addLine(lines, "이용 세부 내용", data.usageDetail());
        addLine(lines, "운영 기관", data.facility());
        addLine(lines, "관리 기관", data.managingAgency());
        addLine(lines, "구분", data.category());
        addLine(lines, "상세 구분", data.detailCategory());
        addLine(lines, "이용 대상", data.target());
        addLine(lines, "장애 유형 조건", data.disabilityCondition());
        addLine(lines, "연령 조건", data.ageCondition());
        addLine(lines, "기타 조건", data.otherCondition());
        addLine(lines, "이용 시간", data.usageTime());
        addLine(lines, "이용 금액", formatMoney(data.fee(), "무료"));
        addLine(lines, "이용 금액 산정 기준", data.feeBasis());
        addLine(lines, "부가 비용", formatMoney(data.additionalCost(), "없음"));
        addLine(lines, "도로명 주소", data.roadAddress());
        addLine(lines, "지번 주소", data.lotAddress());
        addLine(lines, "대표 전화번호", data.contact());
        addLine(lines, "추가 연락처", data.additionalContact());
        addLine(lines, "관리 기관 전화번호", data.managingAgencyContact());
        addLine(lines, "우편번호", data.postalCode());
        addLine(lines, "위도", data.latitude());
        addLine(lines, "경도", data.longitude());
        addLine(lines, "유의사항", data.attention());
        addLine(lines, "데이터 기준일자", dataDate == null ? data.dataDate() : dataDate.toString());
        return String.join("\n", lines);
    }

    private static String formatMoney(Object value, String zeroText) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            long amount = number.longValue();
            return amount == 0
                    ? zeroText
                    : NumberFormat.getIntegerInstance(Locale.KOREA).format(amount) + "원";
        }

        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            BigDecimal amount = new BigDecimal(text.replace(",", ""));
            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                return zeroText;
            }
            BigDecimal normalized = amount.stripTrailingZeros();
            if (normalized.scale() <= 0) {
                return NumberFormat.getIntegerInstance(Locale.KOREA)
                        .format(normalized.longValueExact()) + "원";
            }
            return normalized.toPlainString() + "원";
        } catch (NumberFormatException ignored) {
            return text;
        }
    }

    private static String externalId(String prefix, String identity) {
        return prefix
                + "-"
                + UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));
    }

    private static LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalized = value.trim().replace('.', '-').replace('/', '-');
        try {
            if (normalized.matches("\\d{8}")) {
                return LocalDate.parse(normalized, DateTimeFormatter.BASIC_ISO_DATE);
            }
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static void addLine(List<String> lines, String label, String value) {
        if (StringUtils.hasText(value)) {
            lines.add(label + ": " + value);
        }
    }

    private static String join(String first, String second) {
        if (!StringUtils.hasText(first)) {
            return second;
        }
        if (!StringUtils.hasText(second)) {
            return first;
        }
        return first + " " + second;
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static String limit(String value, int maxLength) {
        return value != null && value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
