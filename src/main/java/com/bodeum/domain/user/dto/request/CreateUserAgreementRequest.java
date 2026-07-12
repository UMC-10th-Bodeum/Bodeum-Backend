package com.bodeum.domain.user.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CreateUserAgreementRequest(
        @NotNull(message = "서비스 이용약관 동의 여부는 필수입니다.")
        Boolean serviceTermsAgreed,

        @NotNull(message = "개인정보처리방침 동의 여부는 필수입니다.")
        Boolean privacyPolicyAgreed,

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
