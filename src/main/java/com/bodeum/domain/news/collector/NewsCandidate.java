package com.bodeum.domain.news.collector;

import com.bodeum.domain.news.entity.NewsCategoryCode;
import com.bodeum.domain.news.entity.NewsType;
import com.bodeum.domain.news.entity.RecruitmentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

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
        LocalDate applyEndDate,
        NewsCategoryCode categoryCode,
        NewsType newsType,
        RecruitmentStatus recruitmentStatus
) {

    public NewsCandidate {
        Objects.requireNonNull(categoryCode, "categoryCode must not be null");
        Objects.requireNonNull(newsType, "newsType must not be null");
        if (!categoryCode.supports(newsType)) {
            throw new IllegalArgumentException(
                    "Category " + categoryCode + " does not support news type " + newsType
            );
        }
    }
}
