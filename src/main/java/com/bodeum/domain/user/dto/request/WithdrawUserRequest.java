package com.bodeum.domain.user.dto.request;

import jakarta.validation.constraints.Size;

public record WithdrawUserRequest(
        @Size(max = 255, message = "탈퇴 사유는 최대 255자까지 입력 가능합니다.")
        String reason
) {
}
