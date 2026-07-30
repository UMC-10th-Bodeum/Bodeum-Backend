package com.bodeum.domain.news.infrastructure.openapi;

import java.util.Map;
import org.springframework.util.StringUtils;

record DisabledWelfareProgramData(
        String sigungu,
        String facility,
        String target,
        String disabilityCondition,
        String ageCondition,
        String otherCondition,
        String category,
        String detailCategory,
        String programName,
        String programContent,
        String usageTime,
        Object fee,
        String feeBasis,
        Object additionalCost,
        String roadAddress,
        String lotAddress,
        String contact,
        String additionalContact,
        String postalCode,
        String latitude,
        String longitude,
        String attention,
        String usageDetail,
        String managingAgency,
        String managingAgencyContact,
        String dataDate
) {

    static DisabledWelfareProgramData fromOdcloud(Map<String, Object> row) {
        return new DisabledWelfareProgramData(
                text(row.get("시군명")),
                text(row.get("복지관명")),
                text(row.get("이용대상")),
                text(row.get("이용대상상세조건(장애유형)")),
                text(row.get("이용대상상세조건(연령제한)")),
                text(row.get("이용대상상세조건(기타조건)")),
                text(row.get("구분")),
                text(row.get("상세구분")),
                text(row.get("프로그램명")),
                text(row.get("프로그램내용")),
                text(row.get("이용시간")),
                firstValue(row, "이용금액", "이용금액(원)"),
                text(row.get("이용금액산정기준")),
                row.get("부가비용"),
                text(row.get("소재지도로명주소")),
                text(row.get("소재지지번주소")),
                firstText(row, "대표전화번호", "전화번호"),
                null,
                text(row.get("우편번호")),
                text(row.get("위도")),
                text(row.get("경도")),
                null,
                null,
                null,
                null,
                firstText(row, "데이터기준일자", "데이터기준일")
        );
    }

    static DisabledWelfareProgramData fromGgOpenApi(Map<String, Object> row) {
        return new DisabledWelfareProgramData(
                text(row.get("SIGUN_NM")),
                text(row.get("CMWELFCT_NM_INFO")),
                text(row.get("USE_TARGET")),
                text(row.get("USE_TARGET_OBSTCL_TYPE_COND")),
                text(row.get("USE_TARGET_AGE_LIMITN_COND")),
                text(row.get("USE_TARGET_ETC_COND")),
                text(row.get("PROG_DIV_NM")),
                text(row.get("DETAIL_DIV_NM")),
                text(row.get("PROG_TITLE")),
                text(row.get("PROG_CONT")),
                text(row.get("USE_TM_INFO")),
                row.get("USE_AMT"),
                text(row.get("USE_AMT_CALC_STD_INFO")),
                row.get("ADDITN_EXPN_INFO"),
                text(row.get("REFINE_ROADNM_ADDR")),
                text(row.get("REFINE_LOTNO_ADDR")),
                text(row.get("TELNO")),
                text(row.get("ADD_CONTCT_NO_INFO")),
                text(row.get("REFINE_ZIPNO")),
                text(row.get("REFINE_WGS84_LAT")),
                text(row.get("REFINE_WGS84_LOGT")),
                text(row.get("ATENTN_MATR")),
                null,
                null,
                null,
                text(row.get("DATA_STD_DE"))
        );
    }

    static DisabledWelfareProgramData fromSasangOdcloud(
            Map<String, Object> row,
            String dataDate
    ) {
        return new DisabledWelfareProgramData(
                "사상구",
                "사상구장애인복지관",
                text(row.get("프로그램대상")),
                null,
                text(row.get("대상")),
                null,
                null,
                null,
                text(row.get("프로그램명")),
                text(row.get("내용")),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                dataDate
        );
    }

    static DisabledWelfareProgramData fromDataGoStandard(Map<String, Object> row) {
        return new DisabledWelfareProgramData(
                firstText(row, "sggNm", "SGG_NM"),
                firstText(row, "wlfcNm", "WLFC_NM"),
                firstText(row, "utztnTrgtNm", "UTZTN_TRGT_NM"),
                null,
                null,
                firstText(row, "utztnTrgtDtlCndNm", "UTZTN_TRGT_DTL_CND_NM"),
                null,
                null,
                firstText(row, "prgrmNm", "PRGRM_NM"),
                firstText(row, "prgrmCn", "PRGRM_CN"),
                usageTime(row),
                firstValue(row, "utztnAmt", "UTZTN_AMT"),
                null,
                firstValue(row, "sbsdCst", "SBSD_CST"),
                firstText(row, "lctnRoadNmAddr", "LCTN_ROAD_NM_ADDR"),
                firstText(row, "lctnLotnoAddr", "LCTN_LOTNO_ADDR"),
                firstText(row, "telno", "TELNO"),
                null,
                null,
                firstText(row, "lat", "LAT"),
                firstText(row, "lot", "LOT"),
                null,
                firstText(row, "utztnDtlCn", "UTZTN_DTL_CN"),
                firstText(row, "mngInstNm", "MNG_INST_NM"),
                firstText(row, "mngInstTelno", "MNG_INST_TELNO"),
                firstText(row, "dataCrtrYmd", "DATA_CRTR_YMD")
        );
    }

    String basicIdentity() {
        return valueOrEmpty(facility) + "|" + valueOrEmpty(programName);
    }

    String detailedIdentity() {
        return String.join(
                "|",
                valueOrEmpty(sigungu),
                valueOrEmpty(facility),
                valueOrEmpty(programName),
                valueOrEmpty(category),
                valueOrEmpty(detailCategory),
                valueOrEmpty(target),
                valueOrEmpty(usageTime)
        );
    }

    private static String usageTime(Map<String, Object> row) {
        String start = firstText(row, "utztnBgngTm", "UTZTN_BGNG_TM");
        String end = firstText(row, "utztnEndTm", "UTZTN_END_TM");
        if (!StringUtils.hasText(start)) {
            return end;
        }
        if (!StringUtils.hasText(end)) {
            return start;
        }
        return start + " ~ " + end;
    }

    private static Object firstValue(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = row.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String firstText(Map<String, Object> row, String... keys) {
        return text(firstValue(row, keys));
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return StringUtils.hasText(text) && !"null".equalsIgnoreCase(text) ? text : null;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
