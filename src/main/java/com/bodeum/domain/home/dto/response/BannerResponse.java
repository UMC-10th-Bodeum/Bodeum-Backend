package com.bodeum.domain.home.dto.response;

import com.bodeum.domain.news.entity.News;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record BannerResponse(
        Long newsId,
        String title,
        Long dDay,
        String summary,
        String linkUrl
) {
    public static BannerResponse from(News news) {
        return new BannerResponse(
                news.getId(),
                news.getTitle(),
                calculateDDay(news.getApplyEndDate()),
                news.getSummary(),
                news.getOriginalUrl()
        );
    }

    private static Long calculateDDay(LocalDate applyEndDate) {
        if (applyEndDate == null) return null;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), applyEndDate);
        return days < 0 ? null : days;
    }
}
