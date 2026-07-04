package com.bodeum.domain.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InterestRegionCreateReqDTO(
        @NotEmpty(message = "관심사를 하나 이상 선택해주세요.")
        @Size(max = 3, message = "관심사는 최대 3개까지 선택 가능합니다.")
        List<String> interests,

        @NotBlank(message = "시/도는 필수입니다.")
        String sido,

        @NotBlank(message = "구/군은 필수입니다.")
        String sigungu
) {
}
