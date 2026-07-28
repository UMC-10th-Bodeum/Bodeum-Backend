package com.bodeum.domain.info.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "카카오지도 URL 생성 요청 DTO")
public record KakaoMapUrlRequest(
        @Schema(description = "정보 항목 ID", example = "1")
        @NotNull(message = "정보 항목 ID는 필수입니다.")
        Long infoItemId
) {
}