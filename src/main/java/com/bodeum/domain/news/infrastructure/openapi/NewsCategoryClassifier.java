package com.bodeum.domain.news.infrastructure.openapi;

import com.bodeum.domain.news.entity.NewsCategoryCode;
import com.bodeum.domain.news.entity.NewsType;
import java.util.Locale;
import org.springframework.util.StringUtils;

final class NewsCategoryClassifier {

    private static final String[] NOTICE_KEYWORDS = {
            "공지", "기관소식", "기관뉴스", "복지뉴스", "센터뉴스", "뉴스레터", "보도자료", "휴관", "휴무",
            "운영중단", "운영변경", "일정안내", "기관안내", "개관"
    };
    private static final String[] VOUCHER_KEYWORDS = {
            "바우처", "지원금", "보조금", "수당", "장학금", "치료비", "의료비", "교육비", "생계비",
            "후원금", "현금지원", "쿠폰지원", "급여지원", "감면", "융자"
    };
    private static final String[] PARTICIPATION_KEYWORDS = {
            "모집", "참여자", "참가자", "캠프", "축제", "대회", "행사", "나들이", "여행", "체험",
            "동아리", "자조모임", "자조활동", "공연", "전시", "관람", "문화활동", "여가활동",
            "레크리에이션", "친구맺기", "간담회", "워크숍", "워크샵"
    };
    private static final String[] EDUCATION_KEYWORDS = {
            "교육", "교실", "강좌", "강의", "세미나", "설명회", "아카데미", "학교", "학습", "훈련",
            "특강", "강습", "코칭", "양성", "배움터", "문해", "공방", "수업", "대학", "스쿨",
            "정보화", "스마트폰", "컴퓨터", "자격증"
    };
    private static final String[] WELFARE_SERVICE_KEYWORDS = {
            "재활", "치료", "복지", "자립", "상담", "돌봄", "보호", "건강", "사례관리", "권익",
            "일자리", "취업", "급식", "목욕", "세탁", "보장구", "활동서비스", "발달", "의료", "심리",
            "지원서비스", "지원사업"
    };

    private NewsCategoryClassifier() {
    }

    static NewsCategoryCode classifyActivity(DisabledWelfareProgramData data) {
        NewsCategoryCode canonicalCode = canonicalActivityCode(data.category());
        if (canonicalCode != null) {
            return canonicalCode;
        }

        String primaryText = normalize(String.join(
                " ",
                valueOrEmpty(data.category()),
                valueOrEmpty(data.detailCategory()),
                valueOrEmpty(data.programName())
        ));
        NewsCategoryCode primaryMatch = classifyText(primaryText);
        if (primaryMatch != null) {
            return primaryMatch;
        }

        String secondaryText = normalize(String.join(
                " ",
                valueOrEmpty(data.programContent()),
                valueOrEmpty(data.usageDetail()),
                valueOrEmpty(data.target())
        ));
        NewsCategoryCode secondaryMatch = classifyText(secondaryText);
        return secondaryMatch == null
                ? NewsCategoryCode.BENEFIT_WELFARE_SERVICE
                : secondaryMatch;
    }

    private static NewsCategoryCode canonicalActivityCode(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            NewsCategoryCode code = NewsCategoryCode.valueOf(
                    value.trim().toUpperCase(Locale.ROOT)
            );
            return code.supports(NewsType.ACTIVITY)
                    ? code
                    : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static NewsCategoryCode classifyText(String text) {
        if (containsAny(text, NOTICE_KEYWORDS)) {
            return NewsCategoryCode.INSTITUTION_NOTICE_NEWS;
        }
        if (containsAny(text, VOUCHER_KEYWORDS)) {
            return NewsCategoryCode.VOUCHER_SUBSIDY;
        }
        if (containsAny(text, PARTICIPATION_KEYWORDS)) {
            return NewsCategoryCode.RECRUITMENT_PARTICIPATION;
        }
        if (containsAny(text, EDUCATION_KEYWORDS)) {
            return NewsCategoryCode.EDUCATION_SEMINAR;
        }
        if (containsAny(text, WELFARE_SERVICE_KEYWORDS)) {
            return NewsCategoryCode.BENEFIT_WELFARE_SERVICE;
        }
        return null;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[·._-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean containsAny(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
