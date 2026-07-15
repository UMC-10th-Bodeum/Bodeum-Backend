package com.bodeum.domain.news.collector;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record NewsCandidate(
        String externalItemId,
        String title,
        String summary,
        String content,
        String sourceName,
        String originalUrl,
        String thumbnailUrl,
        String regionName,
        String targetAudience,
        String contact,
        String manager,
        LocalDateTime publishedAt,
        LocalDate programStartDate,
        LocalDate programEndDate,
        LocalDate applyStartDate,
        LocalDate applyEndDate
) {
}
