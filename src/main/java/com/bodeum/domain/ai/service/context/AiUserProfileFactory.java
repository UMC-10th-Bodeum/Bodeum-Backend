package com.bodeum.domain.ai.service.context;

import com.bodeum.domain.ai.model.rag.AiScrapInterests;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * 사용자 프로필과 최근 스크랩 정보를 기반으로 AI 개인화용 사용자 프로필을 생성한다.
 */
@Component
public class AiUserProfileFactory {

    public AiUserProfile create(
            User user,
            User disabilityProfileUser,
            AiScrapInterests scrapInterests
    ) {
        Region region = user.getRegion();
        return new AiUserProfile(
                region == null ? null : region.getFullName(),
                region == null ? null : region.getRegionLevel1(),
                region == null ? null : region.getRegionLevel2(),
                user.getChildAge(),
                disabilityProfileUser.getDisabilityTypes().stream()
                        .map(Enum::name)
                        .toList(),
                user.getInterestCategories().stream()
                        .map(Enum::name)
                        .toList(),
                user.getKeywordText(),
                scrapInterests.infoTitles(),
                scrapInterests.newsTitles(),
                scrapInterests.communityTopics()
        );
    }
}
