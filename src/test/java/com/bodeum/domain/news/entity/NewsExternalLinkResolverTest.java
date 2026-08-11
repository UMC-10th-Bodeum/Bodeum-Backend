package com.bodeum.domain.news.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NewsExternalLinkResolverTest {

    @Test
    void returnsVerifiedProgramUrlBeforeInstitutionHomepage() {
        String result = NewsExternalLinkResolver.resolve(
                "순천시장애인종합복지관",
                "자연체험활동"
        );

        assertThat(result)
                .isEqualTo("http://www.scrw.or.kr/bbs/view.php?wcode=01&wnum=34208");
    }

    @Test
    void returnsInstitutionHomepageWhenNoProgramUrlIsVerified() {
        String result = NewsExternalLinkResolver.resolve(
                "구리시장애인종합복지관",
                "보행로봇재활"
        );

        assertThat(result).isEqualTo("https://guriwel.or.kr/");
    }

    @Test
    void returnsVerifiedHomepageForHapcheonWelfareCenter() {
        String result = NewsExternalLinkResolver.resolve(
                "합천군장애인복지센터",
                "장애인 평생교육"
        );

        assertThat(result).isEqualTo("https://www.assist.or.kr/");
    }

    @Test
    void keepsSahaHomepageOnItsWorkingHttpAddress() {
        String result = NewsExternalLinkResolver.resolve(
                "사하구장애인종합복지관",
                "직업재활 프로그램"
        );

        assertThat(result).isEqualTo("http://www.saharc.or.kr/");
    }

    @Test
    void keepsExternalUrlEmptyWithoutVerifiedInstitution() {
        String result = NewsExternalLinkResolver.resolve(
                "확인되지 않은 기관",
                "확인되지 않은 프로그램"
        );

        assertThat(result).isNull();
    }
}
