package com.bodeum.domain.ai.infrastructure.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiSiteDomainNormalizerTest {

    @Test
    void keepsDifferentSitesUnderCompoundPublicSuffixSeparate() {
        assertThat(AiSiteDomainNormalizer.normalize("https://news.alpha.co.uk/path"))
                .isEqualTo("alpha.co.uk");
        assertThat(AiSiteDomainNormalizer.normalize("https://shop.beta.co.uk/path"))
                .isEqualTo("beta.co.uk");
    }

    @Test
    void groupsSubdomainsOfSameCompoundDomain() {
        assertThat(AiSiteDomainNormalizer.normalize("https://www.alpha.co.uk"))
                .isEqualTo("alpha.co.uk");
        assertThat(AiSiteDomainNormalizer.normalize("https://m.alpha.co.uk/guide"))
                .isEqualTo("alpha.co.uk");
    }

    @Test
    void groupsKoreanPublicServiceSubdomains() {
        assertThat(AiSiteDomainNormalizer.normalize("https://www.bokjiro.go.kr"))
                .isEqualTo("bokjiro.go.kr");
        assertThat(AiSiteDomainNormalizer.normalize("https://m.bokjiro.go.kr/guide"))
                .isEqualTo("bokjiro.go.kr");
    }

    @Test
    void rejectsPublicSuffixWithoutRegistrableDomain() {
        assertThat(AiSiteDomainNormalizer.normalize("https://co.uk/path")).isNull();
    }

    @Test
    void rejectsMalformedUrl() {
        assertThat(AiSiteDomainNormalizer.normalize("https://exa mple.com")).isNull();
    }
}
