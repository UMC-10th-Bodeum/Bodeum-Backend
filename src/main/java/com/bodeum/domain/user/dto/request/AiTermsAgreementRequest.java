package com.bodeum.domain.user.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record AiTermsAgreementRequest(
        @NotNull(message = "AI 챗봇 이용동의 여부는 필수입니다.")
        @AssertTrue(message = "AI 챗봇 이용동의는 true여야 합니다.")
        Boolean aiTermsAgreed
) {
}
