package com.bodeum.domain.user.dto.response;

import com.bodeum.domain.onboarding.enums.CommunityRoleType;
import com.bodeum.domain.onboarding.enums.GuardianType;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.user.entity.User;
import java.util.List;

public record UserProfileResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        int point,
        int level,
        String badgeName,
        String levelDescription,
        ChildProfile childProfile,
        String keywordText,
        List<CodeLabelResponse> interestCategories,
        Long regionId,
        String regionLevel1,
        String regionLevel2,
        String guardianNickname,
        GuardianType guardianType,
        CommunityRoleType communityRoleType
) {

    public static UserProfileResponse from(User user) {
        Region region = user.getRegion();
        return new UserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getPoint(),
                user.getGuardianLevel().getLevelNumber(),
                user.getGuardianLevel().getBadgeName(),
                user.getGuardianLevel().getDescription(),
                ChildProfile.from(user),
                user.getKeywordText(),
                user.getInterestCategories().stream()
                        .map(CodeLabelResponse::from)
                        .toList(),
                region == null ? null : region.getId(),
                region == null ? null : region.getRegionLevel1(),
                region == null ? null : region.getRegionLevel2(),
                user.getGuardianNickname(),
                user.getGuardianType(),
                user.getCommunityRoleType()
        );
    }

    public record ChildProfile(
            String nickname,
            String birth,
            List<CodeLabelResponse> disabilityTypes
    ) {

        private static ChildProfile from(User user) {
            return new ChildProfile(
                    user.getChildName(),
                    user.getChildBirth(),
                    user.getDisabilityTypes().stream()
                            .map(CodeLabelResponse::from)
                            .toList()
            );
        }
    }
}
