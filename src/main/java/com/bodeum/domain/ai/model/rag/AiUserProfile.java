package com.bodeum.domain.ai.model.rag;

import java.util.List;

public record AiUserProfile(
        String region,
        String regionLevel1,
        String regionLevel2,
        Integer childAge,
        List<String> disabilityTypes,
        List<String> interests,
        String keywordText,
        List<String> scrappedInfoTitles,
        List<String> scrappedNewsTitles,
        List<String> scrappedCommunityTopics,
        AiInfoSubCategory infoSubCategory
) {

    public AiUserProfile {
        disabilityTypes = disabilityTypes == null ? List.of() : List.copyOf(disabilityTypes);
        interests = interests == null ? List.of() : List.copyOf(interests);
        scrappedInfoTitles = copyOfNullable(scrappedInfoTitles);
        scrappedNewsTitles = copyOfNullable(scrappedNewsTitles);
        scrappedCommunityTopics = copyOfNullable(scrappedCommunityTopics);
    }

    public AiUserProfile(
            String region,
            String regionLevel1,
            String regionLevel2,
            Integer childAge,
            List<String> disabilityTypes,
            List<String> interests,
            String keywordText,
            List<String> scrappedInfoTitles,
            List<String> scrappedNewsTitles,
            List<String> scrappedCommunityTopics
    ) {
        this(region, regionLevel1, regionLevel2, childAge, disabilityTypes, interests,
                keywordText, scrappedInfoTitles, scrappedNewsTitles,
                scrappedCommunityTopics, null);
    }

    public AiUserProfile(
            String region,
            String regionLevel1,
            String regionLevel2,
            Integer childAge,
            List<String> disabilityTypes,
            List<String> interests,
            String keywordText
    ) {
        this(
                region,
                regionLevel1,
                regionLevel2,
                childAge,
                disabilityTypes,
                interests,
                keywordText,
                List.of(),
                List.of(),
                List.of(),
                null
        );
    }

    public AiUserProfile withRegion(
            String region,
            String regionLevel1,
            String regionLevel2
    ) {
        return new AiUserProfile(
                region,
                regionLevel1,
                regionLevel2,
                childAge,
                disabilityTypes,
                interests,
                keywordText,
                scrappedInfoTitles,
                scrappedNewsTitles,
                scrappedCommunityTopics,
                infoSubCategory
        );
    }

    public AiUserProfile withInfoSubCategory(AiInfoSubCategory subCategory) {
        return new AiUserProfile(
                region, regionLevel1, regionLevel2, childAge, disabilityTypes,
                interests, keywordText, scrappedInfoTitles, scrappedNewsTitles,
                scrappedCommunityTopics, subCategory
        );
    }

    private static List<String> copyOfNullable(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
