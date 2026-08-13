package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.model.rag.AiScrapInterests;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.user.entity.User;
import org.springframework.stereotype.Component;

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
