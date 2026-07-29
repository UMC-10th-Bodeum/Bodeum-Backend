package com.bodeum.global.infrastructure.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DataGoOpenApiClientTest {

    @Test
    void fetchPageAddsAuthenticationAndPagingParametersAndExtractsArrayItems() {
        DataGoOpenApiProperties properties = properties("test-service-key");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DataGoOpenApiClient client = new DataGoOpenApiClient(builder, properties, false);
        server.expect(requestTo(
                        "https://example.test/programs"
                                + "?serviceKey=test-service-key&pageNo=1&numOfRows=3&type=json"
                ))
                .andRespond(withSuccess("""
                        {
                          "response": {
                            "header": {"resultCode": "00", "resultMsg": "NORMAL_SERVICE"},
                            "body": {
                              "pageNo": "1",
                              "numOfRows": "3",
                              "totalCount": "1",
                              "items": [
                                {"prgrmNm": "예술누림 무빙아트", "sggNm": "금정구"}
                              ]
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        DataGoOpenApiPageResponse response = client.fetchPage("/programs", 1, 3);

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.rows()).singleElement().satisfies(row -> {
            assertThat(row.get("prgrmNm")).isEqualTo("예술누림 무빙아트");
            assertThat(row.get("sggNm")).isEqualTo("금정구");
        });
        server.verify();
    }

    @Test
    void fetchPageAlsoSupportsItemsWrappedByItem() {
        DataGoOpenApiProperties properties = properties("test-service-key");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DataGoOpenApiClient client = new DataGoOpenApiClient(builder, properties, false);
        server.expect(requestTo(
                        "https://example.test/programs"
                                + "?serviceKey=test-service-key&pageNo=2&numOfRows=1&type=json"
                ))
                .andRespond(withSuccess("""
                        {
                          "response": {
                            "header": {"resultCode": "00", "resultMsg": "NORMAL_SERVICE"},
                            "body": {
                              "totalCount": 2,
                              "items": {"item": {"prgrmNm": "단건 프로그램"}}
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        DataGoOpenApiPageResponse response = client.fetchPage("/programs", 2, 1);

        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.rows()).singleElement()
                .extracting(row -> row.get("prgrmNm"))
                .isEqualTo("단건 프로그램");
        server.verify();
    }

    @Test
    void fetchPageRejectsApiErrorResult() {
        DataGoOpenApiProperties properties = properties("test-service-key");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DataGoOpenApiClient client = new DataGoOpenApiClient(builder, properties, false);
        server.expect(requestTo(
                        "https://example.test/programs"
                                + "?serviceKey=test-service-key&pageNo=1&numOfRows=10&type=json"
                ))
                .andRespond(withSuccess("""
                        {
                          "response": {
                            "header": {
                              "resultCode": "30",
                              "resultMsg": "SERVICE_KEY_IS_NOT_REGISTERED_ERROR"
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchPage("/programs", 1, 10))
                .isInstanceOf(PublicDataClientException.class)
                .hasMessageContaining("30", "SERVICE_KEY_IS_NOT_REGISTERED_ERROR");
        server.verify();
    }

    @Test
    void fetchPageRejectsMissingServiceKeyBeforeCallingApi() {
        DataGoOpenApiClient client = new DataGoOpenApiClient(
                RestClient.builder(),
                properties(""),
                false
        );

        assertThatThrownBy(() -> client.fetchPage("/programs", 1, 10))
                .isInstanceOf(PublicDataClientException.class)
                .hasMessageContaining("인증키");
    }

    private DataGoOpenApiProperties properties(String serviceKey) {
        DataGoOpenApiProperties properties = new DataGoOpenApiProperties();
        properties.setBaseUrl("https://example.test");
        properties.setServiceKey(serviceKey);
        return properties;
    }
}
