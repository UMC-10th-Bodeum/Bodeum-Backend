package com.bodeum.domain.onboarding.dto.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateInterestRegionRequest(
        @NotEmpty(message = "관심사를 하나 이상 선택해주세요.")
        @Size(max = 3, message = "관심사는 최대 3개까지 선택 가능합니다.")
        @ArraySchema(schema = @Schema(allowableValues = {"1", "2", "3", "4"}))
        List<@Min(value = 1, message = "지원하지 않는 관심사입니다.")
                @Max(value = 4, message = "지원하지 않는 관심사입니다.") Integer> interestCategoryIds,

        @Schema(example = "1", description = "지역 ID (GET /api/v1/regions 로 조회한 regionId)")
        @NotNull(message = "지역은 필수입니다.")
        Long regionId
) {
}
