package com.bodeum.domain.onboarding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateGuardianProfileRequest(
        @NotBlank(message = "프로필 닉네임은 필수입니다.")
        @Size(max = 20, message = "프로필 닉네임은 최대 20자까지 입력 가능합니다.")
        String guardianNickname,

        @Schema(allowableValues = {"PARENT", "GRANDPARENT", "SIBLING", "OTHER"})
        @Pattern(regexp = "PARENT|GRANDPARENT|SIBLING|OTHER", message = "보호자 유형을 확인해주세요.")
        String guardianType,

        @Schema(allowableValues = {"INFO_SEEKER", "EXPERIENCE_SHARER", "WISDOM_HELPER"})
        @Pattern(regexp = "INFO_SEEKER|EXPERIENCE_SHARER|WISDOM_HELPER", message = "커뮤니티 성향을 확인해주세요.")
        String communityRoleType
) {
}
