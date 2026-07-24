package com.bodeum.domain.mypage.dto.response;

import com.bodeum.domain.onboarding.enums.CommunityRoleType;
import com.bodeum.domain.onboarding.enums.GuardianType;
import com.bodeum.domain.user.dto.response.CodeLabelResponse;
import com.bodeum.domain.user.dto.response.UserProfileResponse;
import java.util.List;

public record MyPageProfileResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        int point,
        int level,
        String badgeName,
        String levelDescription,
        UserProfileResponse.ChildProfile childProfile,
        String keywordText,
        List<CodeLabelResponse> interestCategories,
        Long regionId,
        String regionLevel1,
        String regionLevel2,
        String guardianNickname,
        GuardianType guardianType,
        CommunityRoleType communityRoleType,
        ActivitySummary activitySummary
) {

    public static MyPageProfileResponse of(
            UserProfileResponse profile,
            ActivitySummary activitySummary
    ) {
        return new MyPageProfileResponse(
                profile.userId(),
                profile.nickname(),
                profile.profileImageUrl(),
                profile.point(),
                profile.level(),
                profile.badgeName(),
                profile.levelDescription(),
                profile.childProfile(),
                profile.keywordText(),
                profile.interestCategories(),
                profile.regionId(),
                profile.regionLevel1(),
                profile.regionLevel2(),
                profile.guardianNickname(),
                profile.guardianType(),
                profile.communityRoleType(),
                activitySummary
        );
    }

    public record ActivitySummary(
            long savedInfoCount,
            long myPostCount,
            long myCommentCount
    ) {
    }
}
