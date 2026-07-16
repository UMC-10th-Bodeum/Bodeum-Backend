package com.bodeum.domain.onboarding.dto.request;

import com.bodeum.domain.onboarding.enumtype.CommunityRoleType;
import com.bodeum.domain.onboarding.enumtype.GuardianType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGuardianProfileRequest(
        @NotBlank(message = "프로필 닉네임은 필수입니다.")
        @Size(max = 20, message = "프로필 닉네임은 최대 20자까지 입력 가능합니다.")
        String guardianNickname,

        @Schema(allowableValues = {"PARENT", "GRANDPARENT", "SIBLING", "ETC"})
        GuardianType guardianType,

        @Schema(allowableValues = {"INFO_SEEKER", "EXPERIENCE_SHARER", "WISDOM_HELPER"})
        CommunityRoleType communityRoleType
) {
}
