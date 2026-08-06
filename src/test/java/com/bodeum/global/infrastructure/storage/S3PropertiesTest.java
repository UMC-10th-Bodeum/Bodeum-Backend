package com.bodeum.global.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class S3PropertiesTest {

    private S3Properties properties;

    @BeforeEach
    void setUp() {
        properties = new S3Properties();
        properties.setBucket("bodeum-bucket");
        properties.setRegion("ap-northeast-2");
    }

    @Test
    void acceptsStaticCredentials() {
        properties.setAccessKey("access-key");
        properties.setSecretKey("secret-key");

        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    void acceptsDefaultCredentialsProviderConfiguration() {
        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingBucket() {
        properties.setBucket(" ");

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AWS_S3_BUCKET");
    }

    @Test
    void rejectsMissingRegion() {
        properties.setRegion(null);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AWS_S3_REGION");
    }

    @Test
    void rejectsPartialStaticCredentials() {
        properties.setAccessKey("access-key");

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("둘 다 설정하거나 둘 다 비워야");
    }
}
