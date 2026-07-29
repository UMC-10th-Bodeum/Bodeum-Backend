package com.bodeum.domain.ai.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiUrlNormalizerTest {

    @Test
    void preservesRawEncodedPathAndQueryWhileRemovingFragment() {
        String url = "HTTPS://WWW.NHIS.OR.KR/nhis/%EC%95%88%EB%82%B4"
                + "?categories1=446%2C447%2C448"
                + "&srSearchVal=%EB%B3%B8%EC%9D%B8#section";

        assertThat(AiUrlNormalizer.normalize(url)).isEqualTo(
                "https://www.nhis.or.kr/nhis/%EC%95%88%EB%82%B4"
                        + "?categories1=446%2C447%2C448"
                        + "&srSearchVal=%EB%B3%B8%EC%9D%B8"
        );
    }

    @Test
    void addsHttpsSchemeWhenItIsMissing() {
        assertThat(AiUrlNormalizer.normalize("example.com/path?value=a%2Cb"))
                .isEqualTo("https://example.com/path?value=a%2Cb");
    }
}
