package com.bodeum.domain.user.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(max = 20, message = "닉네임은 최대 20자까지 입력 가능합니다.")
        String nickname,

        @Size(max = 20, message = "자녀 이름은 최대 20자까지 입력 가능합니다.")
        String childName,

        String guardianType
) {
}
