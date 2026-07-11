package com.bodeum.domain.onboarding.dto.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateInterestRegionRequest(
        @NotEmpty(message = "관심사를 하나 이상 선택해주세요.")
        @Size(max = 2, message = "관심사는 최대 2개까지 선택 가능합니다.")
        @ArraySchema(schema = @Schema(allowableValues = {
                "WELFARE_SUBSIDY", "HOSPITAL_HEALTH", "PARENTING_COMMUNICATION", "GROWTH_EDUCATION"
        }))
        List<String> interests,

        @NotBlank(message = "시/도는 필수입니다.")
        String regionLevel1,

        @NotBlank(message = "구/군은 필수입니다.")
        String regionLevel2
) {
}
