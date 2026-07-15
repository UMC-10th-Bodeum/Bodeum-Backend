package com.bodeum.domain.user.dto.response;

import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.enumtype.DisabilityType;
import java.util.List;

public record UserHeaderResponse(
        boolean isLoggedIn,
        String nickname,
        String profileImageUrl,
        Integer level,
        String badgeName,
        List<DisabilityType> childDisabilityTypes,
        Integer childAge,
        String region
) {

    public static UserHeaderResponse loggedOut() {
        return new UserHeaderResponse(false, null, null, null, null, null, null, null);
    }

    public static UserHeaderResponse from(User user) {
        return new UserHeaderResponse(
                true,
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getGuardianLevel().getLevelNumber(),
                user.getGuardianLevel().getBadgeName(),
                user.getDisabilityTypes(),
                user.getChildAge(),
                user.getRegion() == null ? null : user.getRegion().getFullName()
        );
    }
}
