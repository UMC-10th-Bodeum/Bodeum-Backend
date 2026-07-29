package com.bodeum.domain.news.entity;

import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@RequiredArgsConstructor
public enum NewsCategoryCode {

    LOCAL_NEWS(NewsType.LOCAL, "소식", 1),
    LOCAL_POLICY(NewsType.LOCAL, "정책", 2),
    VOUCHER_SUBSIDY(NewsType.ACTIVITY, "바우처 · 지원금", 1),
    RECRUITMENT_PARTICIPATION(NewsType.ACTIVITY, "모집 · 참여", 2),
    EDUCATION_SEMINAR(NewsType.ACTIVITY, "교육 · 세미나", 3),
    BENEFIT_WELFARE_SERVICE(NewsType.ACTIVITY, "혜택 · 복지서비스", 4),
    INSTITUTION_NOTICE_NEWS(NewsType.ACTIVITY, "기관 공지 · 뉴스", 5);

    private final NewsType newsType;
    private final String label;
    private final int sortOrder;

    public boolean supports(NewsType candidateNewsType) {
        return newsType == candidateNewsType;
    }

    public static NewsCategoryCode fromStoredValue(NewsType newsType, String storedValue) {
        if (StringUtils.hasText(storedValue)) {
            try {
                NewsCategoryCode code = valueOf(storedValue.trim().toUpperCase(Locale.ROOT));
                if (code.supports(newsType)) {
                    return code;
                }
            } catch (IllegalArgumentException ignored) {
                // 이전 동적 카테고리명은 아래 호환 규칙으로 변환한다.
            }
        }

        String normalized = normalize(storedValue);
        if (newsType == NewsType.LOCAL) {
            return containsAny(normalized, "정책", "제도", "조례", "행정")
                    ? LOCAL_POLICY
                    : LOCAL_NEWS;
        }
        if (containsAny(normalized, "바우처", "지원금", "보조금", "수당", "장학")) {
            return VOUCHER_SUBSIDY;
        }
        if (containsAny(normalized, "교육", "평생", "세미나", "강좌", "훈련", "교실", "학습", "수업")) {
            return EDUCATION_SEMINAR;
        }
        if (containsAny(normalized, "모집", "참여", "행사", "캠프", "문화", "여가", "체육")) {
            return RECRUITMENT_PARTICIPATION;
        }
        if (containsAny(
                normalized,
                "공지", "기관소식", "기관뉴스", "복지뉴스", "센터뉴스", "뉴스레터", "보도자료", "휴관"
        )) {
            return INSTITUTION_NOTICE_NEWS;
        }
        return BENEFIT_WELFARE_SERVICE;
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT)
                        .replaceAll("[·._-]", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
    }

    private static boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
