package com.bodeum.domain.user.dto.request;

import com.bodeum.domain.user.enumtype.DisabilityType;
import com.bodeum.domain.user.enumtype.InterestCategory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

public record UpdateUserProfileRequest(
        @Schema(example = "민준맘")
        @Size(max = 20, message = "닉네임은 최대 20자까지 입력 가능합니다.")
        String nickname,

        @Schema(example = "민준")
        @Size(max = 20, message = "자녀 닉네임은 최대 20자까지 입력 가능합니다.")
        String childNickname,

        @Schema(example = "2020-03")
        @Pattern(regexp = "\\d{4}-\\d{2}", message = "생년월은 YYYY-MM 형식으로 입력해주세요.")
        String childBirth,

        @ArraySchema(
                arraySchema = @Schema(example = "[\"AUTISM\", \"DEVELOPMENTAL_DELAY\"]"),
                schema = @Schema(
                        type = "string",
                        allowableValues = {
                                "AUTISM",
                                "INTELLECTUAL_DISABILITY",
                                "CEREBRAL_PALSY",
                                "ADHD",
                                "DEVELOPMENTAL_DELAY",
                                "LANGUAGE_DISORDER",
                                "ETC"
                        }
                )
        )
        List<DisabilityType> disabilityTypes,

        @Schema(example = "말이 느림")
        @Size(max = 100, message = "특징 키워드는 최대 100자까지 입력 가능합니다.")
        String keywordText,

        @Size(max = 3, message = "관심사는 최대 3개까지 선택 가능합니다.")
        @ArraySchema(
                arraySchema = @Schema(example = "[\"INSTITUTION\", \"HOSPITAL\", \"WELFARE\"]"),
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
        Long regionId,

        @Schema(allowableValues = {"PARENT", "GRANDPARENT", "SIBLING", "OTHER"})
        @Pattern(regexp = "PARENT|GRANDPARENT|SIBLING|OTHER", message = "보호자 유형을 확인해주세요.")
        String guardianType,

        @Schema(allowableValues = {"INFO_SEEKER", "EXPERIENCE_SHARER", "WISDOM_HELPER"})
        @Pattern(regexp = "INFO_SEEKER|EXPERIENCE_SHARER|WISDOM_HELPER", message = "커뮤니티 성향을 확인해주세요.")
        String communityRoleType
) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "생년월은 YYYY-MM 형식으로 입력해주세요.")
    public boolean isBirthValid() {
        if (childBirth == null) {
            return true;
        }

        try {
            YearMonth birthYearMonth = YearMonth.parse(childBirth);
            return birthYearMonth.getYear() >= 2000 && !birthYearMonth.isAfter(YearMonth.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
