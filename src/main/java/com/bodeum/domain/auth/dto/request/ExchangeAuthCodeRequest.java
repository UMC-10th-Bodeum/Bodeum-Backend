package com.bodeum.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ExchangeAuthCodeRequest(
        @Schema(description = "소셜 로그인 콜백이 프론트로 리다이렉트하며 전달한 일회용 code", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String code
) {
}
