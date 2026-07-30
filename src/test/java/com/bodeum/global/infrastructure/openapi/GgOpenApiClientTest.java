package com.bodeum.global.infrastructure.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GgOpenApiClientTest {

    @Test
    void fetchPageAddsAuthenticationAndPagingParametersAndExtractsRows() {
        GgOpenApiProperties properties = properties("test+service/key=");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GgOpenApiClient client = new GgOpenApiClient(builder, properties);
        server.expect(requestTo(
                        "https://example.test/DspsnCmwelfctOpertProg"
                                + "?KEY=test%2Bservice%2Fkey%3D"
                                + "&Type=json&pIndex=1&pSize=5"
                ))
                .andRespond(withSuccess("""
                        {
                          "DspsnCmwelfctOpertProg": [
                            {
                              "head": [
                                {"list_total_count": 1},
                                {"RESULT": {"CODE": "INFO-000", "MESSAGE": "정상 처리되었습니다."}},
                                {"api_version": "1.0"}
                              ]
                            },
                            {
                              "row": [
                                {"SIGUN_NM": "가평군", "PROG_TITLE": "자립생활 프로그램"}
                              ]
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        GgOpenApiPageResponse response = client.fetchPage(
                "/DspsnCmwelfctOpertProg",
                "DspsnCmwelfctOpertProg",
                1,
                5
        );

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.rows()).singleElement().satisfies(row -> {
            assertThat(row.get("SIGUN_NM")).isEqualTo("가평군");
            assertThat(row.get("PROG_TITLE")).isEqualTo("자립생활 프로그램");
        });
        server.verify();
    }

    @Test
    void fetchPageRejectsApiErrorResult() {
        GgOpenApiProperties properties = properties("test-service-key");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GgOpenApiClient client = new GgOpenApiClient(builder, properties);
        server.expect(requestTo(
                        "https://example.test/DspsnCmwelfctOpertProg"
                                + "?KEY=test-service-key&Type=json&pIndex=1&pSize=5"
                ))
                .andRespond(withSuccess("""
                        {
                          "RESULT": {
                            "CODE": "ERROR-300",
                            "MESSAGE": "인증키가 유효하지 않습니다."
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchPage(
                "/DspsnCmwelfctOpertProg",
                "DspsnCmwelfctOpertProg",
                1,
                5
        ))
                .isInstanceOf(PublicDataClientException.class)
                .hasMessageContaining("ERROR-300", "인증키");
        server.verify();
    }

    @Test
    void fetchPageRejectsMissingServiceKeyBeforeCallingApi() {
        GgOpenApiClient client = new GgOpenApiClient(
                RestClient.builder(),
                properties("")
        );

        assertThatThrownBy(() -> client.fetchPage(
                "/DspsnCmwelfctOpertProg",
                "DspsnCmwelfctOpertProg",
                1,
                5
        ))
                .isInstanceOf(PublicDataClientException.class)
                .hasMessageContaining("인증키");
    }

    private GgOpenApiProperties properties(String serviceKey) {
        GgOpenApiProperties properties = new GgOpenApiProperties();
        properties.setBaseUrl("https://example.test");
        properties.setServiceKey(serviceKey);
        return properties;
    }
}
