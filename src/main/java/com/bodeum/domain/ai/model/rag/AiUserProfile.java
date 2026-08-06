package com.bodeum.domain.ai.model.rag;

import java.util.List;

public record AiUserProfile(
        String region,
        String regionLevel1,
        String regionLevel2,
        Integer childAge,
        List<String> disabilityTypes,
        List<String> interests,
        String keywordText
) {

    public AiUserProfile {
        disabilityTypes = disabilityTypes == null ? List.of() : List.copyOf(disabilityTypes);
        interests = interests == null ? List.of() : List.copyOf(interests);
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
                keywordText
        );
    }
}
