package com.bodeum.domain.user.dto.response;

import com.bodeum.domain.onboarding.enumtype.CareArea;
import com.bodeum.domain.onboarding.enumtype.CommunityRoleType;
import com.bodeum.domain.onboarding.enumtype.GuardianType;
import com.bodeum.domain.onboarding.enumtype.InterestCategory;
import com.bodeum.domain.user.entity.UserAccount;
import java.util.List;

public record UserProfileResponse(
        Long userId,
        String nickname,
        String email,
        String provider,
        String childName,
        Integer childBirthYear,
        Integer childBirthMonth,
        List<CareArea> careAreas,
        String characteristicKeyword,
        List<InterestCategory> interests,
        String regionLevel1,
        String regionLevel2,
        String guardianNickname,
        GuardianType guardianType,
        CommunityRoleType communityRoleType
) {

    public static UserProfileResponse from(UserAccount userAccount) {
        return new UserProfileResponse(
                userAccount.getId(),
                userAccount.getNickname(),
                userAccount.getEmail(),
                userAccount.getProvider().getPath(),
                userAccount.getChildName(),
                userAccount.getChildBirthYear(),
                userAccount.getChildBirthMonth(),
                userAccount.getCareAreas(),
                userAccount.getCharacteristicKeyword(),
                userAccount.getInterests(),
                userAccount.getRegionLevel1(),
                userAccount.getRegionLevel2(),
                userAccount.getGuardianNickname(),
                userAccount.getGuardianType(),
                userAccount.getCommunityRoleType()
        );
    }
}
