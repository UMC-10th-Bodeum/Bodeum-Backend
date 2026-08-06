package com.bodeum.domain.ai.infrastructure.external;

import com.bodeum.domain.ai.entity.AiExternalSource;

public record AiExternalDocumentCandidate(
        AiExternalSource externalSource,
        String title,
        String normalizedUrl,
        String urlHash
) {
}
