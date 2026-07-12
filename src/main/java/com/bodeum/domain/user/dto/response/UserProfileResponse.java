package com.bodeum.domain.user.dto.response;

import com.bodeum.domain.onboarding.enumtype.CommunityRoleType;
import com.bodeum.domain.onboarding.enumtype.GuardianType;
import com.bodeum.domain.user.entity.User;
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
        LocalDateTime updatedAt,
        ChildProfile childProfile,
        String keywordText,
        List<Integer> interestCategoryIds,
        String regionLevel1,
        String regionLevel2,
        String guardianNickname,
        GuardianType guardianType,
        CommunityRoleType communityRoleType
) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                user.getProvider().getPath(),
                user.getProfileImageUrl(),
                user.getPoint(),
                user.getGuardianLevel().getLevelNumber(),
                user.getGuardianLevel().getBadgeName(),
                user.getGuardianLevel().getDescription(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                ChildProfile.from(user),
                user.getKeywordText(),
                user.getInterestCategoryIds(),
                user.getRegionLevel1(),
                user.getRegionLevel2(),
                user.getGuardianNickname(),
                user.getGuardianType(),
                user.getCommunityRoleType()
        );
    }

    public record ChildProfile(
            String nickname,
            String birth,
            List<Integer> disabilityTypeIds
    ) {

        private static ChildProfile from(User user) {
            return new ChildProfile(
                    user.getChildName(),
                    user.getChildBirth(),
                    user.getDisabilityTypeIds()
            );
        }
    }
}
