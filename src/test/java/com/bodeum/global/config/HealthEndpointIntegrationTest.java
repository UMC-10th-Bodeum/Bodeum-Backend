package com.bodeum.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * 헬스 체크 엔드포인트는 Docker HEALTHCHECK와 배포 워크플로가 인증 없이 호출한다.
 * 여기가 막히면 배포 검증이 통째로 실패하므로 공개 여부를 테스트로 고정한다.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "bodeum.auth.jwt-secret=test-jwt-secret-32-bytes-minimum-value"
)
class HealthEndpointIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebEndpointsSupplier webEndpointsSupplier;

    @Test
    @DisplayName("liveness는 인증 없이 200을 반환한다")
    void livenessIsPubliclyAccessible() throws Exception {
        HttpResponse<String> response = get("/actuator/health/liveness");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
    }

    @Test
    @DisplayName("readiness는 인증 없이 200을 반환한다")
    void readinessIsPubliclyAccessible() throws Exception {
        HttpResponse<String> response = get("/actuator/health/readiness");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
    }

    @Test
    @DisplayName("health 응답에 내부 상세 정보를 노출하지 않는다")
    void healthDoesNotExposeDetails() throws Exception {
        HttpResponse<String> response = get("/actuator/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).doesNotContain("components");
    }

    /**
     * HTTP 응답으로는 검증할 수 없다. env 같은 엔드포인트는 permitAll 대상이 아니라
     * 노출 여부와 무관하게 Security가 먼저 401을 돌려주기 때문이다.
     * 그래서 실제로 등록된 엔드포인트 목록을 직접 확인한다.
     */
    @Test
    @DisplayName("웹에 노출된 actuator 엔드포인트는 health 하나뿐이다")
    void onlyHealthEndpointIsExposed() {
        assertThat(webEndpointsSupplier.getEndpoints())
                .extracting(ExposableWebEndpoint::getEndpointId)
                .containsExactly(EndpointId.of("health"));
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();

        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
