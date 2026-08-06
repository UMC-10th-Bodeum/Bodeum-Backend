package com.bodeum.domain.info.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카카오지도 URL 생성 응답 DTO")
public record KakaoMapUrlResponse(
        @Schema(description = "생성된 카카오지도 URL", example = "https://map.kakao.com/link/search/%EB%93%9C%EB%A6%BC%EB%B0%9C%EB%8B%AC%ED%81%B4%EB%A6%AC%EB%8B%89")
        String kakaoMapUrl
) {
    public static KakaoMapUrlResponse from(String kakaoMapUrl) {
        return new KakaoMapUrlResponse(kakaoMapUrl);
    }
}