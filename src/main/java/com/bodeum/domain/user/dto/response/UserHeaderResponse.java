package com.bodeum.domain.user.dto.response;

import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.enums.GuardianLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record UserHeaderResponse(
        boolean isLoggedIn,
        boolean onboardingCompleted,
        String nickname,
        String profileImageUrl,
        Integer level,
        String badgeName,
        List<CodeLabelResponse> childDisabilityTypes,
        Integer childAge,
        @Schema(description = "온보딩에서 저장한 뉴스 기본 지역 ID")
        Long regionId,
        @Schema(description = "온보딩에서 저장한 뉴스 기본 시/도")
        String regionLevel1,
        @Schema(description = "온보딩에서 저장한 뉴스 기본 시/군/구")
        String regionLevel2,
        @Schema(description = "온보딩에서 저장한 뉴스 기본 지역 전체 이름")
        String region
) {

    public static UserHeaderResponse loggedOut() {
        return new UserHeaderResponse(
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static UserHeaderResponse from(User user, int totalPoint) {
        GuardianLevel level = GuardianLevel.from(totalPoint);
        Region region = user.getRegion();
        return new UserHeaderResponse(
                true,
                user.isOnboardingCompleted(),
                user.getNickname(),
                user.getProfileImageUrl(),
                level.getLevelNumber(),
                level.getBadgeName(),
                user.getDisabilityTypes().stream()
                        .map(CodeLabelResponse::from)
                        .toList(),
                user.getChildAge(),
                region == null ? null : region.getId(),
                region == null ? null : region.getRegionLevel1(),
                region == null ? null : region.getRegionLevel2(),
                region == null ? null : region.getFullName()
        );
    }
}
