package com.bodeum.domain.user.dto.response;

public record UserProfileUpdateResponse(
        boolean updated
) {

    public static UserProfileUpdateResponse ofSuccess() {
        return new UserProfileUpdateResponse(true);
    }
}
