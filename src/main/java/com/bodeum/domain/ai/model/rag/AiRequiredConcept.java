package com.bodeum.domain.ai.model.rag;

import com.bodeum.domain.ai.util.AiTextNormalizer;
import java.util.List;

public record AiRequiredConcept(
        String name,
        String retrievalQuery,
        List<String> matchTerms,
        List<String> excludeTerms,
        boolean requiresUserRegion
) {
    public AiRequiredConcept(
            String name,
            String retrievalQuery,
            List<String> matchTerms,
            List<String> excludeTerms
    ) {
        this(name, retrievalQuery, matchTerms, excludeTerms, false);
    }

    public AiRequiredConcept {
        name = AiTextNormalizer.trimToNull(name);
        retrievalQuery = AiTextNormalizer.trimToNull(retrievalQuery);
        matchTerms = normalizeTerms(matchTerms);
        excludeTerms = normalizeTerms(excludeTerms);
    }

    public boolean isValid() {
        return name != null && retrievalQuery != null && !matchTerms.isEmpty();
    }

    private static List<String> normalizeTerms(List<String> terms) {
        return terms == null
                ? List.of()
                : terms.stream()
                        .filter(term -> term != null && !term.isBlank())
                        .map(String::trim)
                        .distinct()
                        .limit(5)
                        .toList();
    }
}
