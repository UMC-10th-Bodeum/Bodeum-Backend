package com.bodeum.domain.news.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NewsCategoryCodeTest {

    @Test
    void convertsLegacyDynamicCategoryNamesToStableCodes() {
        assertThat(NewsCategoryCode.fromStoredValue(NewsType.LOCAL, "SUPPORT_SERVICE"))
                .isEqualTo(NewsCategoryCode.LOCAL_NEWS);
        assertThat(NewsCategoryCode.fromStoredValue(NewsType.ACTIVITY, "교육재활"))
                .isEqualTo(NewsCategoryCode.EDUCATION_SEMINAR);
        assertThat(NewsCategoryCode.fromStoredValue(NewsType.ACTIVITY, "문화예술교육"))
                .isEqualTo(NewsCategoryCode.EDUCATION_SEMINAR);
        assertThat(NewsCategoryCode.fromStoredValue(NewsType.ACTIVITY, "뉴스포츠 교실"))
                .isEqualTo(NewsCategoryCode.EDUCATION_SEMINAR);
        assertThat(NewsCategoryCode.fromStoredValue(NewsType.ACTIVITY, "의료재활"))
                .isEqualTo(NewsCategoryCode.BENEFIT_WELFARE_SERVICE);
    }

    @Test
    void keepsCanonicalCodeOnlyWhenItMatchesTheNewsTab() {
        assertThat(NewsCategoryCode.fromStoredValue(NewsType.ACTIVITY, "LOCAL_POLICY"))
                .isEqualTo(NewsCategoryCode.BENEFIT_WELFARE_SERVICE);
    }
}
