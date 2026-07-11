package com.bodeum.domain.user.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Year;
import java.util.List;

public record UpdateUserProfileRequest(
        @Size(max = 20, message = "닉네임은 최대 20자까지 입력 가능합니다.")
        String nickname,

        @Size(max = 20, message = "자녀 닉네임은 최대 20자까지 입력 가능합니다.")
        String childNickname,

        @Min(value = 2000, message = "출생 연도를 확인해주세요.")
        Integer childBirthYear,

        @Min(value = 1, message = "출생 월은 1월부터 입력 가능합니다.")
        @Max(value = 12, message = "출생 월은 12월까지 입력 가능합니다.")
        Integer childBirthMonth,

        @ArraySchema(schema = @Schema(allowableValues = {
                "AUTISM_SPECTRUM", "INTELLECTUAL", "BRAIN_LESION", "ADHD", "DEVELOPMENTAL", "LANGUAGE", "OTHER"
        }))
        List<String> careAreas,

        @Size(max = 100, message = "특징 키워드는 최대 100자까지 입력 가능합니다.")
        String characteristicKeyword,

        @Size(max = 2, message = "관심사는 최대 2개까지 선택 가능합니다.")
        @ArraySchema(schema = @Schema(allowableValues = {
                "WELFARE_SUBSIDY", "HOSPITAL_HEALTH", "PARENTING_COMMUNICATION", "GROWTH_EDUCATION"
        }))
        List<String> interests,

        @Size(max = 50, message = "시/도는 최대 50자까지 입력 가능합니다.")
        String regionLevel1,

        @Size(max = 50, message = "시/군/구는 최대 50자까지 입력 가능합니다.")
        String regionLevel2,

        @Schema(allowableValues = {"PARENT", "GRANDPARENT", "SIBLING", "OTHER"})
        String guardianType,

        @Schema(allowableValues = {"INFO_SEEKER", "EXPERIENCE_SHARER", "WISDOM_HELPER"})
        String communityRoleType
) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "출생 연도를 확인해주세요.")
    public boolean isBirthYearValid() {
        return childBirthYear == null || childBirthYear <= Year.now().getValue();
    }
}
