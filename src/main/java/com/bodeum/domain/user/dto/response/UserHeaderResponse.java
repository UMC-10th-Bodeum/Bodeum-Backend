package com.bodeum.domain.user.dto.response;

import com.bodeum.domain.onboarding.enumtype.CareArea;
import com.bodeum.domain.user.entity.UserAccount;
import java.util.List;

public record UserHeaderResponse(
        boolean isLoggedIn,
        String nickname,
        String profileImageUrl,
        Integer level,
        String badgeName,
        List<CareArea> childDisabilityTypes,
        Integer childAge,
        String region
) {

    public static UserHeaderResponse loggedOut() {
        return new UserHeaderResponse(false, null, null, null, null, null, null, null);
    }

    public static UserHeaderResponse from(UserAccount userAccount) {
        return new UserHeaderResponse(
                true,
                userAccount.getNickname(),
                userAccount.getProfileImageUrl(),
                userAccount.getGuardianLevel().getLevelNumber(),
                userAccount.getGuardianLevel().getBadgeName(),
                userAccount.getCareAreas(),
                userAccount.getChildAge(),
                buildRegion(userAccount.getRegionLevel1(), userAccount.getRegionLevel2())
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
