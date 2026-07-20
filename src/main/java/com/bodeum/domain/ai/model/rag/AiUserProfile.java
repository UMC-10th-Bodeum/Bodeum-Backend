package com.bodeum.domain.ai.model.rag;

import java.util.List;

public record AiUserProfile(
        String region,
        Integer childAge,
        List<String> disabilityTypes,
        List<String> interests,
        String keywordText
) {
}
