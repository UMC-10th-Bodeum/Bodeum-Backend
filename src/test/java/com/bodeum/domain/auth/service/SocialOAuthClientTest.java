package com.bodeum.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.auth.exception.AuthErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

class SocialOAuthClientTest {

    private final SocialOAuthClient socialOAuthClient = new SocialOAuthClient(
            new OAuthProperties(),
            RestClient.builder()
    );

    @Test
    void parseKakaoProfileIgnoresMalformedNestedObjects() {
        SocialUserProfile profile = ReflectionTestUtils.invokeMethod(
                socialOAuthClient,
                "parseKakaoProfile",
                Map.of(
                        "id", 12345,
                        "kakao_account", "malformed",
                        "properties", 123
                )
        );

        assertThat(profile.providerUserId()).isEqualTo("12345");
        assertThat(profile.email()).isNull();
        assertThat(profile.nickname()).isEqualTo("카카오 사용자");
    }

    @Test
    void parseNaverProfileRejectsMalformedResponse() {
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                socialOAuthClient,
                "parseNaverProfile",
                Map.of("response", "malformed")
        )).isInstanceOf(ProjectException.class);
    }

    @Test
    void tokenExchangeHttpFailureReturnsTokenExchangeError() throws Exception {
        HttpServer server = startServer(exchange -> exchange.sendResponseHeaders(500, -1));

        try {
            SocialOAuthClient client = new SocialOAuthClient(
                    configuredKakaoProperties(server, "/token", "/user-info"),
                    RestClient.builder()
            );

            assertThatThrownBy(() -> client.getUserProfile(SocialProvider.KAKAO, "code", null))
                    .isInstanceOf(ProjectException.class)
                    .extracting(exception -> ((ProjectException) exception).getErrorCode())
                    .isEqualTo(AuthErrorCode.SOCIAL_TOKEN_EXCHANGE_FAILED);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void userInfoHttpFailureReturnsProfileFetchError() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/token", exchange -> writeJson(exchange, "{\"access_token\":\"provider-token\"}"));
        server.createContext("/user-info", exchange -> exchange.sendResponseHeaders(500, -1));
        server.start();

        try {
            SocialOAuthClient client = new SocialOAuthClient(
                    configuredKakaoProperties(server, "/token", "/user-info"),
                    RestClient.builder()
            );

            assertThatThrownBy(() -> client.getUserProfile(SocialProvider.KAKAO, "code", null))
                    .isInstanceOf(ProjectException.class)
                    .extracting(exception -> ((ProjectException) exception).getErrorCode())
                    .isEqualTo(AuthErrorCode.SOCIAL_PROFILE_FETCH_FAILED);
        } finally {
            server.stop(0);
        }
    }

    private HttpServer startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/token", handler);
        server.start();
        return server;
    }

    private OAuthProperties configuredKakaoProperties(HttpServer server, String tokenPath, String userInfoPath) {
        OAuthProperties properties = new OAuthProperties();
        OAuthProperties.ProviderRegistration kakao = properties.getKakao();
        kakao.setClientId("test-kakao-client");
        kakao.setTokenUri(serverUrl(server, tokenPath));
        kakao.setUserInfoUri(serverUrl(server, userInfoPath));
        return properties;
    }

    private String serverUrl(HttpServer server, String path) {
        return "http://localhost:" + server.getAddress().getPort() + path;
    }

    private void writeJson(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
