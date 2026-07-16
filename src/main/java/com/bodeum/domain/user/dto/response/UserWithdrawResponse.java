package com.bodeum.domain.user.dto.response;

public record UserWithdrawResponse(
        boolean success
) {

    public static UserWithdrawResponse ofSuccess() {
        return new UserWithdrawResponse(true);
    }
}
