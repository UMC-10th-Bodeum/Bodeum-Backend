package com.bodeum.domain.user.dto.response;

import com.bodeum.domain.onboarding.enumtype.CommunityRoleType;
import com.bodeum.domain.onboarding.enumtype.GuardianType;
import com.bodeum.domain.user.entity.UserAccount;
import java.time.LocalDateTime;
import java.util.List;

public record UserProfileResponse(
        Long userId,
        String nickname,
        String email,
        String provider,
        String profileImageUrl,
        int point,
        int level,
        String badgeName,
        String levelDescription,
        LocalDateTime joinedAt,
        ChildProfile childProfile,
        String keywordText,
        List<Integer> interestCategoryIds,
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
                userAccount.getProfileImageUrl(),
                userAccount.getPoint(),
                userAccount.getGuardianLevel().getLevelNumber(),
                userAccount.getGuardianLevel().getBadgeName(),
                userAccount.getGuardianLevel().getDescription(),
                userAccount.getCreatedAt(),
                ChildProfile.from(userAccount),
                userAccount.getKeywordText(),
                userAccount.getInterestCategoryIds(),
                userAccount.getRegionLevel1(),
                userAccount.getRegionLevel2(),
                userAccount.getGuardianNickname(),
                userAccount.getGuardianType(),
                userAccount.getCommunityRoleType()
        );
    }

    public record ChildProfile(
            String nickname,
            String birth,
            List<Integer> disabilityTypeIds
    ) {

        private static ChildProfile from(UserAccount userAccount) {
            return new ChildProfile(
                    userAccount.getChildName(),
                    userAccount.getChildBirth(),
                    userAccount.getDisabilityTypeIds()
            );
        }
    }
}
