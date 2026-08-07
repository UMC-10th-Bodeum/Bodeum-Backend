package com.bodeum.domain.ai.model.rag;

import java.util.List;

public record AiScrapInterests(
        List<String> infoTitles,
        List<String> newsTitles,
        List<String> communityTopics
) {

    public AiScrapInterests {
        infoTitles = copyOfNullable(infoTitles);
        newsTitles = copyOfNullable(newsTitles);
        communityTopics = copyOfNullable(communityTopics);
    }

    public static AiScrapInterests empty() {
        return new AiScrapInterests(List.of(), List.of(), List.of());
    }

    private static List<String> copyOfNullable(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
