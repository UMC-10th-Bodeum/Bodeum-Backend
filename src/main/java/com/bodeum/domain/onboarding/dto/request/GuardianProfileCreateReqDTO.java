package com.bodeum.domain.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuardianProfileCreateReqDTO(
        @NotBlank(message = "보호자 닉네임은 필수입니다.")
        @Size(max = 20, message = "보호자 닉네임은 최대 20자까지 입력 가능합니다.")
        String guardianNickname,

        @NotBlank(message = "보호자 유형은 필수입니다.")
        String guardianType,

        @NotBlank(message = "커뮤니티 역할 성향은 필수입니다.")
        String communityRoleType
) {
}
