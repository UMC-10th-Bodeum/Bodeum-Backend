package com.bodeum.global.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        // 1. 커넥션 풀 설정
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(200);           // 전체 최대 커넥션 수
        connectionManager.setDefaultMaxPerRoute(50); // 호스트(IP)당 최대 커넥션 수

        // 2. 타임아웃 설정 (공공데이터 API 서버 지연 대응을 위해 증대)
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(5))           // 연결 타임아웃 5초
                .setResponseTimeout(Timeout.ofSeconds(20))         // 응답 대기(Read) 타임아웃 20초 (기존 5초 -> 20초)
                .setConnectionRequestTimeout(Timeout.ofSeconds(10)) // 커넥션 풀 대기 타임아웃 10초
                .build();

        // 3. HttpClient 생성
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .build();

        // 4. RequestFactory 연결
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return RestClient.builder()
                .requestFactory(requestFactory);
    }

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}