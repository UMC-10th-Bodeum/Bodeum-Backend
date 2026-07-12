package com.bodeum.domain.user.dto.response;

import com.bodeum.domain.user.entity.User;
import java.util.List;

public record UserHeaderResponse(
        boolean isLoggedIn,
        String nickname,
        String profileImageUrl,
        Integer level,
        String badgeName,
        List<Integer> childDisabilityTypeIds,
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
                user.getDisabilityTypeIds(),
                user.getChildAge(),
                buildRegion(user.getRegionLevel1(), user.getRegionLevel2())
        );
    }

    private static String buildRegion(String regionLevel1, String regionLevel2) {
        if (regionLevel1 == null && regionLevel2 == null) {
            return null;
        }
        if (regionLevel1 == null) {
            return regionLevel2;
        }
        if (regionLevel2 == null) {
            return regionLevel1;
        }
        return regionLevel1 + " " + regionLevel2;
    }
}
