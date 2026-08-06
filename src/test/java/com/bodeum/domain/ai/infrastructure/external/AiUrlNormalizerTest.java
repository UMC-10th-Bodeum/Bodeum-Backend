package com.bodeum.domain.ai.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bodeum.global.apiPayload.exception.ProjectException;
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

    @Test
    void separatesHostAndPortWhenSchemeIsMissing() {
        assertThat(AiUrlNormalizer.normalize("example.com:8080/path"))
                .isEqualTo("https://example.com:8080/path");
    }

    @Test
    void rejectsUrlContainingUserInfo() {
        assertThatThrownBy(() ->
                AiUrlNormalizer.normalize("https://user:password@example.com/path"))
                .isInstanceOf(ProjectException.class)
                .hasRootCauseMessage("URL userinfo is not allowed");
    }

    @Test
    void rejectsNonHttpSchemes() {
        assertThatThrownBy(() -> AiUrlNormalizer.normalize("ftp://example.com/file"))
                .isInstanceOf(ProjectException.class)
                .hasRootCauseMessage("URL scheme must be http or https");
        assertThatThrownBy(() -> AiUrlNormalizer.normalize("javascript://example.com/path"))
                .isInstanceOf(ProjectException.class)
                .hasRootCauseMessage("URL scheme must be http or https");
    }

    @Test
    void doesNotDuplicateIpv6Brackets() {
        assertThat(AiUrlNormalizer.normalize("http://[::1]:8000/path"))
                .isEqualTo("http://[::1]:8000/path");
    }
}
