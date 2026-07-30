package com.bodeum.global.infrastructure.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OdcloudClientTest {

    @Test
    void fetchPageAddsPagingAndServiceKey() {
        OdcloudProperties properties = properties("test+service/key=");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OdcloudClient client = new OdcloudClient(builder, properties);
        server.expect(requestTo(
                        "https://example.test/api/dataset?page=1&perPage=10"
                                + "&serviceKey=test%2Bservice%2Fkey%3D"
                ))
                .andRespond(withSuccess("""
                        {
                          "page": 1,
                          "perPage": 10,
                          "totalCount": 1,
                          "currentCount": 1,
                          "data": [{"지정기관명": "테스트 기관", "이용금액": 5000, "부가비용": null}]
                        }
                        """, MediaType.APPLICATION_JSON));

        OdcloudPageResponse response = client.fetchPage("/dataset", 1, 10);

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.data()).singleElement()
                .extracting(row -> row.get("지정기관명"))
                .isEqualTo("테스트 기관");
        assertThat(response.data().getFirst().get("이용금액")).isEqualTo(5000);
        assertThat(response.data().getFirst().get("부가비용")).isNull();
        server.verify();
    }

    @Test
    void fetchPageRejectsMissingServiceKeyBeforeCallingApi() {
        OdcloudProperties properties = properties("");
        OdcloudClient client = new OdcloudClient(RestClient.builder(), properties);

        assertThatThrownBy(() -> client.fetchPage("/dataset", 1, 10))
                .isInstanceOf(PublicDataClientException.class)
                .hasMessageContaining("인증키");
    }

    private OdcloudProperties properties(String serviceKey) {
        OdcloudProperties properties = new OdcloudProperties();
        properties.setBaseUrl("https://example.test/api");
        properties.setServiceKey(serviceKey);
        return properties;
    }
}
