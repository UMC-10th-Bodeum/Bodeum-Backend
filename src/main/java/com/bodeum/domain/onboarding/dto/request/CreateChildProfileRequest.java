package com.bodeum.domain.onboarding.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Year;
import java.util.List;

public record CreateChildProfileRequest(
        @Size(max = 20, message = "자녀 이름은 최대 20자까지 입력 가능합니다.")
        String childName,

        @NotNull(message = "자녀 출생 연도는 필수입니다.")
        @Min(value = 2000, message = "출생 연도를 확인해주세요.")
        Integer birthYear,

        @NotNull(message = "자녀 출생 월은 필수입니다.")
        @Min(value = 1, message = "출생 월은 1월부터 입력 가능합니다.")
        @Max(value = 12, message = "출생 월은 12월까지 입력 가능합니다.")
        Integer birthMonth,

        @NotEmpty(message = "집중 케어 영역을 하나 이상 선택해주세요.")
        @ArraySchema(schema = @Schema(allowableValues = {
                "AUTISM_SPECTRUM", "INTELLECTUAL", "BRAIN_LESION", "ADHD", "DEVELOPMENTAL", "LANGUAGE", "OTHER"
        }))
        List<String> careAreas,

        @Size(max = 100, message = "특징 키워드는 최대 100자까지 입력 가능합니다.")
        String characteristicKeyword
) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "출생 연도를 확인해주세요.")
    public boolean isBirthYearValid() {
        return birthYear == null || birthYear <= Year.now().getValue();
    }
}
