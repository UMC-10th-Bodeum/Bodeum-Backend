package com.bodeum.global.config;

import com.bodeum.global.infrastructure.constant.OpenApiSourceSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;

// 외부 API 서버에 요청을 보내기 전, 인증 키와 페이징을 주소에 붙여줌.

@Component
public class OpenApiUrlBuilder {

    @Value("${openapi.secret.datago}")
    private String datagoKey;

    @Value("${openapi.secret.odcloud}")
    private String odcloudKey;

    @Value("${openapi.secret.gg}")
    private String ggKey;

    // OpenApiSourceSpec의 플랫폼 타입에 맞춰 인증키 파라미터를 동적으로 결합한 최종 URI 생성.
    public URI buildUri(OpenApiSourceSpec spec, int page, int size) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(spec.getBaseUrl());

        switch (spec.getUrlType()) {
            case "DATAGO" -> builder
                    .queryParam("serviceKey", datagoKey)
                    .queryParam("pageNo", page)
                    .queryParam("numOfRows", size)
                    .queryParam("_type", "json"); // JSON 응답 강제

            case "ODCLOUD" -> builder
                    .queryParam("page", page)
                    .queryParam("perPage", size)
                    .queryParam("serviceKey", odcloudKey);

            case "GG" -> builder
                    .queryParam("KEY", ggKey)
                    .queryParam("pIndex", page)
                    .queryParam("pSize", size)
                    .queryParam("Type", "json");

            default -> {
                // ETC 등 별도 인증이 당장 필요 없는 케이스 처리
            }
        }

        // 공공데이터포털 등 디코딩된 키 인코딩 깨짐 방지를 위해 인코딩 모드 적용 파싱
        return builder.build().encode(StandardCharsets.UTF_8).toUri();
    }
}