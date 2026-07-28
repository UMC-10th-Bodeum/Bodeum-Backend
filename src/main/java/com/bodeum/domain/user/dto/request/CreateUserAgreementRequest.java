package com.bodeum.domain.user.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CreateUserAgreementRequest(
        @Schema(description = "서비스 이용약관 동의 여부 (필수, true여야 함)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "서비스 이용약관 동의 여부는 필수입니다.")
        Boolean serviceTermsAgreed,

        @Schema(description = "개인정보처리방침 동의 여부 (필수, true여야 함)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "개인정보처리방침 동의 여부는 필수입니다.")
        Boolean privacyPolicyAgreed,

        @Schema(description = "AI 챗봇 이용 동의 여부 (선택, 생략 시 미동의로 처리)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Boolean aiTermsAgreed
) {

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasAgreedRequiredTerms() {
        return Boolean.TRUE.equals(serviceTermsAgreed) && Boolean.TRUE.equals(privacyPolicyAgreed);
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isAiTermsAgreedValue() {
        return Boolean.TRUE.equals(aiTermsAgreed);
    }
}
