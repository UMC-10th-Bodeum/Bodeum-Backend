package com.bodeum.global.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        return RestClient.builder()
                .requestFactory(requestFactory);
    }

    @Bean
    public RestClient openApiClient(RestClient.Builder sharedBuilder) {

        // 공공데이터포털 서비스키 인코딩 깨짐 방지 대책
        DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();
        uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        // 빌더 조립, 스프링 부트가 내장한 기본 컨버터 시스템이 JSON과 XML(gradle에 선언됨)을 알아서 Map 변환.
        return sharedBuilder
                .uriBuilderFactory(uriBuilderFactory)
                .build();
    }
}
