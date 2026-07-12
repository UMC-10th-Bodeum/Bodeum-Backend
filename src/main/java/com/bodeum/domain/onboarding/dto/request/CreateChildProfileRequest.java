package com.bodeum.domain.onboarding.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

public record CreateChildProfileRequest(
        @Schema(example = "민준")
        @Size(max = 20, message = "자녀 닉네임은 최대 20자까지 입력 가능합니다.")
        String childNickname,

        @Schema(example = "2020-03")
        @NotBlank(message = "자녀 생년월은 필수입니다.")
        @Pattern(regexp = "\\d{4}-\\d{2}", message = "생년월은 YYYY-MM 형식으로 입력해주세요.")
        String birth,

        @NotEmpty(message = "집중 케어 영역을 하나 이상 선택해주세요.")
        @ArraySchema(
                arraySchema = @Schema(example = "[1, 3]"),
                schema = @Schema(type = "integer", allowableValues = {"1", "2", "3", "4", "5", "6", "7"})
        )
        List<@Min(value = 1, message = "지원하지 않는 케어 영역입니다.")
                @Max(value = 7, message = "지원하지 않는 케어 영역입니다.") Integer> disabilityTypeIds,

        @Schema(example = "말이 느림")
        @Size(max = 100, message = "특징 키워드는 최대 100자까지 입력 가능합니다.")
        String keywordText
) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "생년월은 YYYY-MM 형식으로 입력해주세요.")
    public boolean isBirthValid() {
        try {
            YearMonth birthYearMonth = YearMonth.parse(birth);
            return birthYearMonth.getYear() >= 2000 && !birthYearMonth.isAfter(YearMonth.now());
        } catch (DateTimeParseException | NullPointerException e) {
            return false;
        }
    }
}
