package com.bodeum.domain.user.dto.request;

import com.bodeum.domain.onboarding.enums.CommunityRoleType;
import com.bodeum.domain.onboarding.enums.GuardianType;
import com.bodeum.domain.user.enums.DisabilityType;
import com.bodeum.domain.user.enums.InterestCategory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.hibernate.validator.constraints.UniqueElements;

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
        @UniqueElements(message = "집중 케어 영역은 중복 선택할 수 없습니다.")
        List<DisabilityType> disabilityTypes,

        @Schema(example = "말이 느림")
        @Size(max = 100, message = "특징 키워드는 최대 100자까지 입력 가능합니다.")
        String keywordText,

        @Size(max = 2, message = "관심사는 최대 2개까지 선택 가능합니다.")
        @UniqueElements(message = "관심사는 중복 선택할 수 없습니다.")
        @ArraySchema(
                arraySchema = @Schema(example = "[\"WELFARE_SUBSIDY\", \"HOSPITAL_HEALTH\"]"),
                schema = @Schema(
                        type = "string",
                        allowableValues = {
                                "WELFARE_SUBSIDY",
                                "HOSPITAL_HEALTH",
                                "PARENTING_COMMUNICATION",
                                "GROWTH_EDUCATION"
                        }
                )
        )
        List<InterestCategory> interestCategories,

        @Schema(example = "1", description = "지역 ID")
        Long regionId,

        @Schema(allowableValues = {"PARENT", "GRANDPARENT", "SIBLING", "ETC"})
        GuardianType guardianType,

        @Schema(allowableValues = {"INFO_SEEKER", "EXPERIENCE_SHARER", "WISDOM_HELPER"})
        CommunityRoleType communityRoleType
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
