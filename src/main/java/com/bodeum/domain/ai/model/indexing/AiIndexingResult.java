package com.bodeum.domain.ai.model.indexing;

public record AiIndexingResult(
        int infoSourceCount,
        int newsSourceCount,
        int documentCount
) {
}
