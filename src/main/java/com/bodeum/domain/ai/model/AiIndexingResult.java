package com.bodeum.domain.ai.model;

public record AiIndexingResult(
        int infoSourceCount,
        int newsSourceCount,
        int documentCount
) {
}
