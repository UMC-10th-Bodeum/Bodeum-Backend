package com.bodeum.domain.ai.model.context;

import com.bodeum.domain.ai.model.rag.AiSourceKey;
import java.util.List;
import java.util.Set;

public record AiAdditionalResultsContext(
        String previousQuestion,
        Set<AiSourceKey> excludedSources,
        List<String> excludedTitles,
        Set<String> excludedIdentityKeys
) {
    public static AiAdditionalResultsContext empty() {
        return new AiAdditionalResultsContext(null, Set.of(), List.of(), Set.of());
    }

    public boolean isFollowUp() {
        return previousQuestion != null && !previousQuestion.isBlank();
    }
}
