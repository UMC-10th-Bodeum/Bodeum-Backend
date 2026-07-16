package com.bodeum.domain.onboarding.dto.request;

import com.bodeum.domain.user.enumtype.InterestCategory;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.hibernate.validator.constraints.UniqueElements;

public record CreateInterestRegionRequest(
        @NotEmpty(message = "관심사를 하나 이상 선택해주세요.")
        @Size(max = 2, message = "관심사는 최대 2개까지 선택 가능합니다.")
        @UniqueElements(message = "관심사는 중복 선택할 수 없습니다.")
        @ArraySchema(
                arraySchema = @Schema(example = "[\"INSTITUTION\", \"HOSPITAL\"]"),
                schema = @Schema(
                        type = "string",
                        allowableValues = {
                                "INSTITUTION",
                                "HOSPITAL",
                                "WELFARE",
                                "EMPLOYMENT",
                                "EDUCATION"
                        }
                )
        )
        List<InterestCategory> interestCategories,

        @Schema(example = "1", description = "지역 ID")
        @NotNull(message = "지역은 필수입니다.")
        Long regionId
) {
}
