package com.bodeum.domain.user.dto.response;

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
        List<String> careAreas,
        String characteristicKeyword,
        List<String> interests,
        String sido,
        String sigungu,
        String guardianNickname,
        String guardianType,
        String communityRoleType
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
                userAccount.getSido(),
                userAccount.getSigungu(),
                userAccount.getGuardianNickname(),
                userAccount.getGuardianType(),
                userAccount.getCommunityRoleType()
        );
    }
}
